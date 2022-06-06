package muffin.app

import scala.quoted.*

import io.circe.Decoder

import cats.{Applicative, MonadThrow}

class RouterBuilder[F[_]: MonadThrow,
  ActionClasses <: Tuple,
  ActionCallbacks <: Tuple,
  ActionMethods <: Tuple,
  CommandClasses <: Tuple,
  CommandMethods <: Tuple,
  DialogClasses <: Tuple,
  DialogMethods <: Tuple] private(
                                   unexpectedAction: (String, RawAction) => F[AppResponse],
                                   unexpectedCommand: (String, CommandContext) => F[AppResponse],
                                   unexpectedDialog: (String, DialogContext) => F[AppResponse]
                                 ) {

  def action[ActionClass, ActionCallback, ActionMethod <: Singleton] =
    new RouterBuilder[
      F,
      ActionClass *: ActionClasses,
      ActionCallback *: ActionCallbacks,
      ActionMethod *: ActionMethods,
      CommandClasses,
      CommandMethods,
      DialogClasses,
      DialogMethods
    ](unexpectedAction, unexpectedCommand, unexpectedDialog)

  def command[CommandClass, CommandMethod <: Singleton] =
    new RouterBuilder[
      F,
      ActionClasses,
      ActionCallbacks,
      ActionMethods,
      CommandClass *: CommandClasses,
      CommandMethod *: CommandMethods,
      DialogClasses,
      DialogMethods
    ](unexpectedAction, unexpectedCommand, unexpectedDialog)

  def dialog[DialogClass, DialogMethod <: Singleton] =
    new RouterBuilder[
      F,
      ActionClasses,
      ActionCallbacks,
      ActionMethods,
      CommandClasses,
      CommandMethods,
      DialogClass *: DialogClasses,
      DialogMethod *: DialogMethods
    ](unexpectedAction, unexpectedCommand, unexpectedDialog)

  def unexpected(
                  unexpectedAction: (String, RawAction) => F[AppResponse] = RouterBuilder.defaultInvalidError,
                  unexpectedCommand: (String, CommandContext) => F[AppResponse] = RouterBuilder.defaultInvalidError,
                  unexpectedDialog: (String, DialogContext) => F[AppResponse] = RouterBuilder.defaultInvalidError,
                ) =
    new RouterBuilder[
      F,
      ActionClasses,
      ActionCallbacks,
      ActionMethods,
      CommandClasses,
      CommandMethods,
      DialogClasses,
      DialogMethods
    ](unexpectedAction, unexpectedCommand, unexpectedDialog)

  inline def build[G[_]]: G[Router[F]] = ${
    RouterBuilder.generator[
    F,
    G,
    ActionClasses,
    ActionCallbacks,
    ActionMethods,
    CommandClasses,
    CommandMethods,
    DialogClasses,
    DialogMethods
  ]('unexpectedAction, 'unexpectedCommand, 'unexpectedDialog)
  }

}

object RouterBuilder {
  def apply[F[_]: MonadThrow]: RouterBuilder[
    F,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple
  ] = new RouterBuilder[
    F,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple,
    EmptyTuple
  ](defaultInvalidError[F, RawAction], defaultInvalidError[F, CommandContext], defaultInvalidError[F, DialogContext])

  private def defaultInvalidError[F[_] : MonadThrow, T]: (String, T) => F[AppResponse] = (_, _) => MonadThrow[F].pure(AppResponse.Ok())

  private def summonNames[Method <: Tuple](method: Method)(using
    Quotes
  ): List[String] =
    method match
      case (head: String) *: tail =>
        head :: summonNames(tail)
      case head *: _ =>
        quotes.reflect.report.errorAndAbort(
          s"Can't get value from: $head isn't String"
        )
      case EmptyTuple => Nil

  private def getMethod[T](
    name: String
  )(using q: Quotes)(using Type[T]): q.reflect.Symbol = {
    import q.reflect.*
    TypeTree
      .of[T]
      .tpe
      .classSymbol
      .map(_.declaredMethod(name))
      .toList
      .flatten match {
      case head :: Nil => head
      case head :: _ =>
        report.errorAndAbort(
          s"Router builder doesn't support overloading $head"
        )
      case _ =>
        report.errorAndAbort(s"Can't find $name in ${Type.show[T]}")
    }
  }

  private def defineMethod[F[_]](using q: Quotes)(
    cls: q.reflect.Symbol,
    name: String,
    params: List[(String, q.reflect.TypeRepr)],
    response: q.reflect.TypeRepr
  ) = {
    import q.reflect.*
    val (paramNames, paramTypes) = params.unzip
    Symbol.newMethod(
      cls,
      name,
      MethodType(paramNames)(_ => paramTypes, _ => response)
    )
  }

  private def makeMethod[F[_], T](using q: Quotes)(
    cls: q.reflect.Symbol,
    methodName: String,
    names: List[String],
    cases: (
      List[String],
      q.reflect.Term,
      q.reflect.Term
    ) => List[q.reflect.Term],
    unexpectedBody: Expr[(String, T) => F[AppResponse]]
  )(using Type[F], Type[T]) = {
    import q.reflect.*

    val stringTypeTree = TypeTree.of[String]
    val bind = Symbol.newBind(Symbol.spliceOwner, "unexpected", Flags.EmptyFlags, stringTypeTree.tpe)

    val defaultPattern = Bind(bind, Typed(Ref(bind), stringTypeTree))

    val symbol = cls.declaredMethod(methodName)
      .headOption
      .getOrElse(
        report
          .errorAndAbort(s"Can't find $methodName symbol in generated class")
      )

    DefDef(symbol,
      {
        case List(List(name: Term, action: Term)) =>
          val body =
            if (names.nonEmpty)
              Match(
                name,
                names.zip(cases(names, name, action)).map { case (name, body) =>
                  CaseDef(Literal(StringConstant(name)), None, body)
                } :+ CaseDef(defaultPattern, None, '{${unexpectedBody}.apply(${name.asExprOf[String]}, ${action.asExprOf[T]})}.asTerm)
              )
            else
              Expr
                .summon[Applicative[F]]
                .map(applicative =>
                  '{
                    Applicative[F](${ applicative })
                      .pure[AppResponse](AppResponse.Ok())
                  }.asTerm
                )
                .getOrElse(
                  report.errorAndAbort(
                    s"Could not summon ${Type.show[Applicative[F]]}"
                  )
                )

          Some(Block(Nil, body).changeOwner(symbol))
        case _ =>
          report.errorAndAbort(s"Invalid generated signature $methodName")
      }
    )
  }

  def summonActionCases[F[_], Class <: Tuple, Callback <: Tuple](using
    q: Quotes
  )(names: List[String], name: q.reflect.Term, action: q.reflect.Term)(using
    Type[F],
    Type[Class],
    Type[Callback]
  ): List[q.reflect.Term] = {
    import quotes.reflect.*

    (Type.of[Class], Type.of[Callback]) match {
      case ('[headClass *: tailClass], '[headCallback *: tailCallback]) =>
        (
          Expr.summon[headClass],
          Expr.summon[Decoder[headCallback]],
          Expr.summon[MonadThrow[F]]
        ) match {
          case (Some(handler), Some(decoder), Some(monad)) =>
            val method = getMethod[headClass](names.head)

            '{
              ${ monad }.flatMap(${ action.asExprOf[RawAction] }
                .asTyped[F, headCallback](${ monad }, ${ decoder }))(action =>
                ${
                  handler.asTerm
                    .select(method)
                    .appliedTo('{ action }.asTerm)
                    .asExprOf[F[AppResponse]]
                }
              )
            }.asTerm :: summonActionCases[F, tailClass, tailCallback](
              names.tail,
              name,
              action
            )

          case (_, _, _) =>
            report.errorAndAbort(
              s"Could not summon ${Type.show[headClass]} or ${Type
                  .show[Decoder[headCallback]]} or ${Type.show[MonadThrow[F]]}"
            )
        }
      case _ => Nil
    }
  }

  private def summonCommandCases[F[_], Class <: Tuple](using
    q: Quotes
  )(names: List[String], name: q.reflect.Term, action: q.reflect.Term)(using
    Type[F],
    Type[Class]
  ): List[q.reflect.Term] = {
    import quotes.reflect.*

    Type.of[Class] match {
      case '[headClass *: tailClass] =>
        Expr.summon[headClass] match {
          case Some(handler) =>
            handler.asTerm
              .select(getMethod[headClass](names.head))
              .appliedTo(action) ::
              summonCommandCases[F, tailClass](names.tail, name, action)
          case _ =>
            report.errorAndAbort(s"Could not summon ${Type.show[headClass]}")
        }
      case _ => Nil
    }
  }

  private def summonDialogCases[F[_], Class <: Tuple](using
    q: Quotes
  )(names: List[String], name: q.reflect.Term, action: q.reflect.Term)(using
    Type[F],
    Type[Class]
  ): List[q.reflect.Term] = {
    import quotes.reflect.*

    Type.of[Class] match {
      case '[headClass *: tailClass] =>
        Expr.summon[headClass] match {
          case Some(handler) =>
            handler.asTerm
              .select(getMethod[headClass](names.head))
              .appliedTo(action) ::
              summonDialogCases[F, tailClass](names.tail, name, action)
          case _ =>
            report.errorAndAbort(s"Could not summon ${Type.show[headClass]}")
        }
      case _ => Nil
    }
  }

  def generator[F[_], G[_],
    ActionClasses <: Tuple,
    ActionCallbacks <: Tuple,
    ActionMethods <: Tuple,
    CommandClasses <: Tuple,
    CommandMethods <: Tuple,
    DialogClasses <: Tuple,
    DialogMethods <: Tuple](
      unexpectedAction: Expr[(String, RawAction) => F[AppResponse]],
      unexpectedCommand: Expr[(String, CommandContext) => F[AppResponse]],
      unexpectedDialog: Expr[(String, DialogContext) => F[AppResponse]]
    )(
    using
    Type[F],
    Type[G],
    Type[ActionClasses],
    Type[ActionCallbacks],
    Type[ActionMethods],
    Type[CommandClasses],
    Type[CommandMethods],
    Type[DialogClasses],
    Type[DialogMethods],
    Quotes
  ): Expr[G[Router[F]]] = {
    import quotes.reflect.*

    val actionMethods: List[String] = Type
      .valueOfTuple[ActionMethods]
      .map(summonNames(_))
      .getOrElse(
        report
          .errorAndAbort(s"Can't get values from: ${Type.show[ActionMethods]}")
      )
    val commandMethods: List[String] = Type
      .valueOfTuple[CommandMethods]
      .map(summonNames(_))
      .getOrElse(
        report
          .errorAndAbort(s"Can't get values from: ${Type.show[CommandMethods]}")
      )
    val dialogMethods: List[String] = Type
      .valueOfTuple[DialogMethods]
      .map(summonNames(_))
      .getOrElse(
        report
          .errorAndAbort(s"Can't get values from: ${Type.show[DialogMethods]}")
      )

    val routerMethods = (cls: Symbol) =>
      defineMethod(
        cls,
        "handleAction",
        ("actionName" -> TypeRepr.of[String]) ::
          ("context" -> TypeRepr.of[RawAction]) :: Nil,
        TypeRepr.of[F[AppResponse]]
      ) ::
        defineMethod(
          cls,
          "handleCommand",
          ("actionName" -> TypeRepr.of[String]) ::
            ("context" -> TypeRepr.of[CommandContext]) :: Nil,
          TypeRepr.of[F[AppResponse]]
        ) ::
        defineMethod(
          cls,
          "handleDialog",
          ("actionName" -> TypeRepr.of[String]) ::
            ("context" -> TypeRepr.of[DialogContext]) :: Nil,
          TypeRepr.of[F[AppResponse]]
        ) ::
        Nil

    val cls = Symbol.newClass(
      Symbol.spliceOwner,
      "__Router__",
      parents = List(
        TypeTree.of[Object].tpe,
        TypeTree.of[Router].tpe.appliedTo(TypeRepr.of[F])
      ),
      routerMethods,
      selfType = None
    )

    val methods =
      makeMethod[F, RawAction](
        cls,
        "handleAction",
        actionMethods,
        summonActionCases[F, ActionClasses, ActionCallbacks](_, _, _),
        unexpectedAction
      ) ::
        makeMethod[F, CommandContext](
          cls,
          "handleCommand",
          commandMethods,
          summonCommandCases[F, CommandClasses](_, _, _),
          unexpectedCommand
        ) ::
        makeMethod[F, DialogContext](
          cls,
          "handleDialog",
          dialogMethods,
          summonDialogCases[F, DialogClasses](_, _, _),
          unexpectedDialog
        ) ::
        Nil

    val clsDef = ClassDef(
      cls,
      List(TypeTree.of[Object], TypeTree.of[Router[F]]),
      body = methods
    )

    val monad = Expr.summon[MonadThrow[G]].getOrElse(
      report.errorAndAbort(s"Can't summon: ${Type.show[MonadThrow[G]]}")
    )

    val newCls = Typed(
      New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone,
      TypeTree.of[Router[F]]
    )

    val block = Block(List(clsDef), '{${monad}.pure(${newCls.asExprOf[Router[F]]})}.asTerm).asExprOf[G[Router[F]]]

//    report.error(block.asTerm.show)

    block
  }

}

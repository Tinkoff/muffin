package muffin.app

import scala.quoted.*

import io.circe.Decoder

import cats.{Applicative, MonadThrow}

class RouterBuilder[F[
  _
], ActionClasses <: Tuple, ActionCallbacks <: Tuple, ActionMethods <: Tuple, CommandClasses <: Tuple, CommandMethods <: Tuple, DialogClasses <: Tuple, DialogMethods <: Tuple] private () {

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
    ]()

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
    ]()

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
    ]()

  inline def build: Router[F] = ${
    RouterBuilder.generator[
      F,
      ActionClasses,
      ActionCallbacks,
      ActionMethods,
      CommandClasses,
      CommandMethods,
      DialogClasses,
      DialogMethods
    ]
  }

}

object RouterBuilder {
  def apply[F[_]]: RouterBuilder[
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
  ]()

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
      case head :: xs =>
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

  private def makeMethod[F[_]](using q: Quotes)(
    cls: q.reflect.Symbol,
    methodName: String,
    names: List[String],
    cases: (
      List[String],
      q.reflect.Term,
      q.reflect.Term
    ) => List[q.reflect.Term]
  )(using Type[F]) = {
    import q.reflect.*
    DefDef(
      cls
        .declaredMethod(methodName)
        .headOption
        .getOrElse(
          report
            .errorAndAbort(s"Can't find $methodName symbol in generated class")
        ),
      {
        case List(List(name: Term, action: Term)) =>
          val body =
            if (names.nonEmpty)
              Match(
                name,
                names.zip(cases(names, name, action)).map { case (name, body) =>
                  CaseDef(Literal(StringConstant(name)), None, body)
                }
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

          Some(Block(Nil, body))
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
                .asTyped[F, headCallback](${
                  name.asExprOf[String]
                })(${ monad }, ${ decoder }))(pair =>
                ${
                  handler.asTerm
                    .select(method)
                    .appliedTo('{ pair._1 }.asTerm, '{ pair._2 }.asTerm)
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
              .appliedTo(name, action) ::
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
              .appliedTo(name, action) ::
              summonDialogCases[F, tailClass](names.tail, name, action)
          case _ =>
            report.errorAndAbort(s"Could not summon ${Type.show[headClass]}")
        }
      case _ => Nil
    }
  }

  def generator[F[
    _
  ], ActionClasses <: Tuple, ActionCallbacks <: Tuple, ActionMethods <: Tuple, CommandClasses <: Tuple, CommandMethods <: Tuple, DialogClasses <: Tuple, DialogMethods <: Tuple](
    using
    Type[F],
    Type[ActionClasses],
    Type[ActionCallbacks],
    Type[ActionMethods],
    Type[CommandClasses],
    Type[CommandMethods],
    Type[DialogClasses],
    Type[DialogMethods],
    Quotes
  ): Expr[Router[F]] = {
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
      makeMethod(
        cls,
        "handleAction",
        actionMethods,
        summonActionCases[F, ActionClasses, ActionCallbacks](_, _, _)
      ) ::
        makeMethod(
          cls,
          "handleCommand",
          commandMethods,
          summonCommandCases[F, CommandClasses](_, _, _)
        ) ::
        makeMethod(
          cls,
          "handleDialog",
          dialogMethods,
          summonDialogCases[F, DialogClasses](_, _, _)
        ) ::
        Nil

    val clsDef = ClassDef(
      cls,
      List(TypeTree.of[Object], TypeTree.of[Router[F]]),
      body = methods
    )
    val newCls = Typed(
      Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil),
      TypeTree.of[Router[F]]
    )

    val block = Block(List(clsDef), newCls).asExprOf[Router[F]]

//    report.error(block.asTerm.show)

    block
  }

}

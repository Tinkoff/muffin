package muffin.internal.macros

import scala.quoted.*

import cats.MonadThrow

import muffin.codec.{Decode, Encode}
import muffin.internal.router.*
import muffin.model.*
import muffin.router.*

class RouterMacro[F[_]: Type, G[_]: Type, T: Type](
    tree: Expr[T],
    monadThrowF: Expr[MonadThrow[F]],
    monadThrowG: Expr[MonadThrow[G]],
    onDialogMissing: Expr[HttpAction => AppResponse[Nothing]],
    onActionMissing: Expr[HttpAction => AppResponse[Nothing]],
    onCommandMissing: Expr[CommandAction => AppResponse[Nothing]]
)(using q: Quotes)
  extends MacroUtils {

  import q.reflect.*

  private case class MatchCase(handle: String, pattern: String, body: Term)

  private case class HandleDef(
      actions: Term => List[MatchCase],
      commands: Term => List[MatchCase],
      dialogs: Term => List[MatchCase]
  )

  private def defineMethod(
      cls: Symbol,
      params: List[(String, TypeRepr)],
      response: TypeRepr,
      cases: List[Term => List[MatchCase]]
  ): Term = {
    val (paramNames, paramTypes) = params.unzip
    quotes
      .reflect
      .Lambda(
        cls,
        MethodType(paramNames)(_ => paramTypes, _ => response),
        {
          case (sym, List(actionName: Term, rawAction: Term)) =>
            if (cases.isEmpty)
              '{
                ${
                  monadThrowF
                }.pure[AppResponse[Nothing]](AppResponse.Ok())
              }.asTerm
            else {

              val body = Match(
                actionName,
                cases
                  .flatMap(_.apply(rawAction))
                  .map {
                    case MatchCase(handler, name, body) => CaseDef(Literal(StringConstant(s"$handler::$name")), None, body)
                  }
              )
              Block(Nil, body).changeOwner(sym)
            }

          case _ => report.errorAndAbort("Invalid method signature")
        }
      )
  }

  private def flatTypeTree[T: Type](path: Term): List[HandleDef] =
    Type.of[T] match {
      case '[left <> right] =>
        flatTypeTree[left](path.select(getSymbolFromType[left <> right]("left"))) ++
        flatTypeTree[right](path.select(getSymbolFromType[left <> right]("right")))
      case '[Handle[
            ?,
            h,
            n,
            commandName,
            commandOut,
            actionName,
            actionIn,
            actionOut,
            dialogName,
            dialogIn,
            dialogOut
          ]] =>
        val handle = getSymbolFromType[T]("h")
        flatTypeTreeHandle[
          h,
          n,
          commandName,
          commandOut,
          actionName,
          actionIn,
          actionOut,
          dialogName,
          dialogIn,
          dialogOut
        ](path, handle)
    }

  private def flatTypeTreeHandle[
      H: Type,
      N: Type,
      CommandName <: Tuple: Type,
      CommandOut <: Tuple: Type,
      ActionName <: Tuple: Type,
      ActionIn <: Tuple: Type,
      ActionOut <: Tuple: Type,
      DialogName <: Tuple: Type,
      DialogIn <: Tuple: Type,
      DialogOut <: Tuple: Type
  ](path: Term, handle: Symbol): List[HandleDef] = {

    val handlerName = summonValue[N].toString()
    val actions     = summonNames[ActionName]
    val commands    = summonNames[CommandName]
    val dialogs     = summonNames[DialogName]

    val actionCases =
      (rawAction: Term) =>
        actions
          .zip(summonActionCases[H, ActionIn, ActionOut](path.select(handle), actions, rawAction))
          .map(MatchCase.apply(handlerName, _, _))

    val commandCases =
      (rawAction: Term) =>
        commands.zip(summonCommandCases[H, CommandOut](path.select(handle), commands, rawAction))
          .map(MatchCase.apply(handlerName, _, _))

    val dialogCases =
      (rawAction: Term) =>
        dialogs
          .zip(summonDialogCases[H, DialogIn, DialogOut](path.select(handle), dialogs, rawAction))
          .map(MatchCase.apply(handlerName, _, _))

    HandleDef(actionCases, commandCases, dialogCases) :: Nil
  }

  private def summonActionCases[H: Type, In <: Tuple: Type, Out <: Tuple: Type](
      handler: Term,
      names: List[String],
      rawAction: Term
  ): List[Term] =
    (Type.of[In], Type.of[Out]) match {
      case ('[headIn *: tailIn], '[headOut *: tailOut]) =>
        val decoder = summonGiven[Decode[MessageAction[headIn]]]
        val encoder = summonGiven[Encode[AppResponse[headOut]]]

        val method = getMethodSymbol[H](names.head)

        val rawActionExpr = rawAction.asExprOf[HttpAction]

        '{
          $monadThrowF.map {
            $monadThrowF.flatMap($monadThrowF.fromEither($decoder.apply($rawActionExpr.data))) { action =>
              ${
                handler
                  .select(method)
                  .appliedTo(
                    '{
                      action
                    }.asTerm
                  )
                  .asExprOf[F[AppResponse[headOut]]]
              }
            }
          }(res => HttpResponse($encoder.apply(res)))
        }.asTerm :: summonActionCases[H, tailIn, tailOut](handler, names.tail, rawAction)

      case _ => Nil
    }

  private def summonDialogCases[H: Type, In <: Tuple: Type, Out <: Tuple: Type](
      handler: Term,
      names: List[String],
      rawAction: Term
  ): List[Term] =
    (Type.of[In], Type.of[Out]) match {
      case ('[headIn *: tailIn], '[headOut *: tailOut]) =>
        val decoder = summonGiven[Decode[DialogAction[headIn]]]
        val encoder = summonGiven[Encode[AppResponse[headOut]]]

        val method = getMethodSymbol[H](names.head)

        val rawActionExpr = rawAction.asExprOf[HttpAction]

        '{
          $monadThrowF.map {
            $monadThrowF.flatMap($monadThrowF.fromEither($decoder.apply($rawActionExpr.data))) { dialog =>
              ${
                handler
                  .select(method)
                  .appliedTo(
                    '{
                      dialog
                    }.asTerm
                  )
                  .asExprOf[F[AppResponse[headOut]]]
              }
            }
          }(res => HttpResponse($encoder.apply(res)))
        }.asTerm :: summonDialogCases[H, tailIn, tailOut](handler, names.tail, rawAction)

      case _ => Nil
    }

  private def summonCommandCases[H: Type, Out <: Tuple: Type](
      handler: Term,
      names: List[String],
      rawAction: Term
  ): List[Term] =
    Type.of[Out] match {
      case '[headOut *: tailOut] =>
        val encoder = summonGiven[Encode[AppResponse[headOut]]]

        val method = getMethodSymbol[H](names.head)

        '{
          $monadThrowF.map(
            ${
              handler.select(method).appliedTo(rawAction).asExprOf[F[AppResponse[headOut]]]
            }
          )(res => HttpResponse($encoder.apply(res)))
        }.asTerm :: summonCommandCases[H, tailOut](handler, names.tail, rawAction)

      case _ => Nil
    }

  def createRouter: Expr[G[Router[F]]] = {
    val (actions, commands, dialogs) = flatTypeTree[T](tree.asTerm).unzip3(h => (h.actions, h.commands, h.dialogs))

    val owner = quotes.reflect.Symbol.spliceOwner

    val actionLambda = defineMethod(
      owner,
      ("actionName" -> TypeRepr.of[String]) :: ("context" -> TypeRepr.of[HttpAction]) :: Nil,
      TypeRepr.of[F[HttpResponse]],
      actions
    ).asExprOf[(String, HttpAction) => F[HttpResponse]]

    val commandLambda = defineMethod(
      owner,
      ("actionName" -> TypeRepr.of[String]) :: ("context" -> TypeRepr.of[CommandAction]) :: Nil,
      TypeRepr.of[F[HttpResponse]],
      commands
    ).asExprOf[(String, CommandAction) => F[HttpResponse]]

    val dialogLambda = defineMethod(
      owner,
      ("actionName" -> TypeRepr.of[String]) :: ("context" -> TypeRepr.of[HttpAction]) :: Nil,
      TypeRepr.of[F[HttpResponse]],
      dialogs
    ).asExprOf[(String, HttpAction) => F[HttpResponse]]

    '{
      $monadThrowG.pure(
        RouterMacro.ofLambda[F](
          ${
            actionLambda
          },
          ${
            commandLambda
          },
          ${
            dialogLambda
          }
        )
      )
    }
  }

}

object RouterMacro {

  def router[F[_]: Type, G[_]: Type, T <: RouterDSL: Type](
      tree: Expr[T],
      monadThrowF: Expr[MonadThrow[F]],
      monadThrowG: Expr[MonadThrow[G]],
      onDialogMissing: Expr[HttpAction => AppResponse[Nothing]],
      onActionMissing: Expr[HttpAction => AppResponse[Nothing]],
      onCommandMissing: Expr[CommandAction => AppResponse[Nothing]]
  )(using Quotes): Expr[G[Router[F]]] =
    new RouterMacro[F, G, T](tree, monadThrowF, monadThrowG, onDialogMissing, onActionMissing, onCommandMissing)
      .createRouter

  def ofLambda[F[_]](
      actionLambda: (String, HttpAction) => F[HttpResponse],
      commandLambda: (String, CommandAction) => F[HttpResponse],
      handleLambda: (String, HttpAction) => F[HttpResponse]
  ): Router[F] =
    new Router[F] {

      def handleAction(actionName: String, context: HttpAction): F[HttpResponse] = actionLambda(actionName, context)

      def handleCommand(actionName: String, context: CommandAction): F[HttpResponse] =
        commandLambda(
          actionName,
          context
        )

      def handleDialog(actionName: String, context: HttpAction): F[HttpResponse] = handleLambda(actionName, context)

    }

}

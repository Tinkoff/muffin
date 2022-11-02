package muffin.internal.router

import scala.quoted.*

import muffin.model.{CommandAction, *}
import muffin.router.HttpAction

sealed trait RouterDSL

case class <>[Left <: RouterDSL, Right <: RouterDSL](left: Left, right: Right) extends RouterDSL

case class Handle[F[
    _
], H, N <: Singleton, CommandName <: Tuple, CommandOut <: Tuple, ActionName <: Tuple, ActionIn <: Tuple, ActionOut <: Tuple, DialogName <: Tuple, DialogIn <: Tuple, DialogOut <: Tuple](
    h: H
) extends RouterDSL

object Handle {

  extension [F[
      _
  ], H, N <: Singleton, CommandName <: Tuple, CommandOut <: Tuple, ActionName <: Tuple, ActionIn <: Tuple, ActionOut <: Tuple, DialogName <: Tuple, DialogIn <: Tuple, DialogOut <: Tuple](
      h: Handle[F, H, N, CommandName, CommandOut, ActionName, ActionIn, ActionOut, DialogName, DialogIn, DialogOut]
  ) {

    transparent inline def command[Out](
        inline value: H => CommandAction => F[AppResponse[Out]]
    ): Handle[F, H, N, ? <: Tuple, Out *: CommandOut, ActionName, ActionIn, ActionOut, DialogName, DialogIn, DialogOut] =
      ${
        Handle.command[
          F,
          H,
          N,
          Out,
          CommandName,
          CommandOut,
          ActionName,
          ActionIn,
          ActionOut,
          DialogName,
          DialogIn,
          DialogOut
        ](
          'value,
          '{
            h.h
          }
        )
      }

    transparent inline def action[In, Out](
        inline value: H => MessageAction[In] => F[AppResponse[Out]]
    ): Handle[F, H, N, CommandName, CommandOut, ? <: Tuple, In *: ActionIn, Out *: ActionOut, DialogName, DialogIn, DialogOut] =
      ${
        Handle.action[
          F,
          H,
          N,
          In,
          Out,
          CommandName,
          CommandOut,
          ActionName,
          ActionIn,
          ActionOut,
          DialogName,
          DialogIn,
          DialogOut
        ](
          'value,
          '{
            h.h
          }
        )
      }

    transparent inline def dialog[In, Out](
        inline value: H => DialogAction[In] => F[AppResponse[Out]]
    ): Handle[F, H, N, CommandName, CommandOut, ActionName, ActionIn, ActionOut, ? <: Tuple, In *: DialogIn, Out *: DialogOut] =
      ${
        Handle.dialog[
          F,
          H,
          N,
          In,
          Out,
          CommandName,
          CommandOut,
          ActionName,
          ActionIn,
          ActionOut,
          DialogName,
          DialogIn,
          DialogOut
        ](
          'value,
          '{
            h.h
          }
        )
      }

  }

  private def functionName(using Quotes)(term: quotes.reflect.Term): Type[?] = {
    import quotes.reflect.*
    term match {
      case Block(List(DefDef(_, _, _, Some(Block(List(DefDef(_, _, _, Some(Apply(Select(_, name), _)))), _)))), _) =>
        ConstantType(StringConstant(name)).asType
      case _                                                                                                       =>
        report.errorAndAbort(
          "Can't extract function name, only '.command(_.fun)' or '.command(h => h.fun)' syntax supported"
        )
    }
  }

  def command[F[_]
    : Type, H: Type, N <: Singleton: Type, Out: Type, CommandName <: Tuple: Type, CommandOut <: Tuple: Type, ActionName <: Tuple: Type, ActionIn <: Tuple: Type, ActionOut <: Tuple: Type, DialogName <: Tuple: Type, DialogIn <: Tuple: Type, DialogOut <: Tuple: Type](
      fun: Expr[H => CommandAction => F[AppResponse[Out]]],
      handle: Expr[H]
  )(using Quotes) = {
    import quotes.reflect.*

    functionName(fun.asTerm.underlying) match {
      case '[name] =>
        '{
          Handle[
            F,
            H,
            N,
            name *: CommandName,
            Out *: CommandOut,
            ActionName,
            ActionIn,
            ActionOut,
            DialogName,
            DialogIn,
            DialogOut
          ]($handle)
        }
    }
  }

  def action[F[_]
    : Type, H: Type, N <: Singleton: Type, In: Type, Out: Type, CommandName <: Tuple: Type, CommandOut <: Tuple: Type, ActionName <: Tuple: Type, ActionIn <: Tuple: Type, ActionOut <: Tuple: Type, DialogName <: Tuple: Type, DialogIn <: Tuple: Type, DialogOut <: Tuple: Type](
      fun: Expr[H => MessageAction[In] => F[AppResponse[Out]]],
      handle: Expr[H]
//      name: Expr[N]
  )(using Quotes) = {
    import quotes.reflect.*

    functionName(fun.asTerm.underlying) match {
      case '[name] =>
        '{
          Handle[
            F,
            H,
            N,
            CommandName,
            CommandOut,
            name *: ActionName,
            In *: ActionIn,
            Out *: ActionOut,
            DialogName,
            DialogIn,
            DialogOut
          ]($handle)
        }
    }
  }

  def dialog[F[_]
    : Type, H: Type, N <: Singleton: Type, In: Type, Out: Type, CommandName <: Tuple: Type, CommandOut <: Tuple: Type, ActionName <: Tuple: Type, ActionIn <: Tuple: Type, ActionOut <: Tuple: Type, DialogName <: Tuple: Type, DialogIn <: Tuple: Type, DialogOut <: Tuple: Type](
      fun: Expr[H => DialogAction[In] => F[AppResponse[Out]]],
      handle: Expr[H]
//      name: Expr[N]
  )(using Quotes) = {
    import quotes.reflect.*

    functionName(fun.asTerm.underlying) match {
      case '[name] =>
        '{
          Handle[
            F,
            H,
            N,
            CommandName,
            CommandOut,
            ActionName,
            ActionIn,
            ActionOut,
            name *: DialogName,
            In *: DialogIn,
            Out *: DialogOut
          ]($handle)
        }
    }
  }

}

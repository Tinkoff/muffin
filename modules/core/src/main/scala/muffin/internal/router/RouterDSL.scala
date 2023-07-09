package muffin.internal.router

import scala.quoted.*

import muffin.model.{CommandAction, *}

sealed trait RouterDSL

case class <>[Left <: RouterDSL, Right <: RouterDSL](left: Left, right: Right) extends RouterDSL

case class Handle[F[
    _
], H, N <: Singleton, CommandName <: Tuple, ActionName <: Tuple, ActionIn <: Tuple, DialogName <: Tuple, DialogIn <: Tuple](
    h: H
) extends RouterDSL

object Handle {

  extension [F[
      _
  ], H, N <: Singleton, CommandName <: Tuple, ActionName <: Tuple, ActionIn <: Tuple, DialogName <: Tuple, DialogIn <: Tuple](
      h: Handle[F, H, N, CommandName, ActionName, ActionIn, DialogName, DialogIn]
  ) {

    transparent inline def command(
        inline value: H => CommandAction => F[AppResponse]
    ): Handle[F, H, N, ? <: Tuple, ActionName, ActionIn, DialogName, DialogIn] =
      ${
        Handle.command[
          F,
          H,
          N,
          CommandName,
          ActionName,
          ActionIn,
          DialogName,
          DialogIn,
        ](
          'value,
          '{
            h.h
          }
        )
      }

    transparent inline def action[In](
        inline value: H => MessageAction[In] => F[AppResponse]
    ): Handle[F, H, N, CommandName, ? <: Tuple, In *: ActionIn, DialogName, DialogIn] =
      ${
        Handle.action[
          F,
          H,
          N,
          In,
          CommandName,
          ActionName,
          ActionIn,
          DialogName,
          DialogIn,
        ](
          'value,
          '{
            h.h
          }
        )
      }

    transparent inline def dialog[In](
        inline value: H => DialogAction[In] => F[AppResponse]
    ): Handle[F, H, N, CommandName, ActionName, ActionIn, ? <: Tuple, In *: DialogIn] =
      ${
        Handle.dialog[
          F,
          H,
          N,
          In,
          CommandName,
          ActionName,
          ActionIn,
          DialogName,
          DialogIn,
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
    : Type, H: Type, N <: Singleton: Type, CommandName <: Tuple: Type, ActionName <: Tuple: Type, ActionIn <: Tuple: Type, DialogName <: Tuple: Type, DialogIn <: Tuple: Type](
      fun: Expr[H => CommandAction => F[AppResponse]],
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
            ActionName,
            ActionIn,
            DialogName,
            DialogIn,
          ]($handle)
        }
    }
  }

  def action[F[_]
    : Type, H: Type, N <: Singleton: Type, In: Type, CommandName <: Tuple: Type, ActionName <: Tuple: Type, ActionIn <: Tuple: Type, DialogName <: Tuple: Type, DialogIn <: Tuple: Type](
      fun: Expr[H => MessageAction[In] => F[AppResponse]],
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
            CommandName,
            name *: ActionName,
            In *: ActionIn,
            DialogName,
            DialogIn,
          ]($handle)
        }
    }
  }

  def dialog[F[_]
    : Type, H: Type, N <: Singleton: Type, In: Type, CommandName <: Tuple: Type, ActionName <: Tuple: Type, ActionIn <: Tuple: Type, DialogName <: Tuple: Type, DialogIn <: Tuple: Type](
      fun: Expr[H => DialogAction[In] => F[AppResponse]],
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
            CommandName,
            ActionName,
            ActionIn,
            name *: DialogName,
            In *: DialogIn
          ]($handle)
        }
    }
  }

}

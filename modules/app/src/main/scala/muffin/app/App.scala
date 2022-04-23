package muffin.app

import cats.{Applicative, Monad}
import io.circe.{Decoder, Json}
import muffin.predef.*

import scala.collection.mutable
import scala.reflect.ClassTag
import cats.syntax.all.given

case class CommandContext(
  channelId: ChannelId,
  channelName: String,
  responseUrl: String, // TODO URL
  teamDomain: String,
  teamId: String, // TODO ID,
  text: Option[String],
  triggerId: String,
  userId: UserId,
  userName: String
)

case class DialogContext(
)

case class ActionContext(action: Json)

sealed trait AppResponse

object AppResponse {
  case class Ok() extends AppResponse

}

class App[F[_]: Monad](val ctx: AppContext[F]) {

  type CommandActions = (AppContext[F], CommandContext) => F[AppResponse]

  type DialogActions = (AppContext[F], DialogContext) => F[AppResponse]

  type EventActions = (AppContext[F], ActionContext) => F[AppResponse]

  private val commands: mutable.Map[String, CommandActions] = mutable.Map.empty

  private val dialogs: mutable.Map[String, DialogActions] = mutable.Map.empty

  private val events: mutable.Map[String, EventActions] = mutable.Map.empty

  def command(name: String)(action: CommandActions): App[F] =
    commands += name -> action
    this

  def dialog(name: String)(action: DialogActions): App[F] =
    dialogs += name -> action
    this

  def actions[T](
    name: String
  )(action: EventActions): App[F] = {
    events += name -> action
    this
  }




  def routeAction(ac:Any): F[AppResponse] = {

//      commands.get("").map(action => action(ctx, CommandContext(???)))
//
//    dialogs.get("").map(action => action(ctx, CommandContext(???)))
//
//    events.get("").map(action => action(ctx, CommandContext(???)))

    ???
  }



}

object App {}

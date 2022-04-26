package muffin.app

import cats.{Applicative, Monad}
import io.circe.{Codec, Decoder, Json}
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
  callback_id: String,
  state: String,
  user_id: UserId,
  channel_id: ChannelId,
  team_id: String,
  submission: Map[String, String]
) derives Codec.AsObject

case class ActionContext(
  user_id: String,
  user_name: String,
  channel_id: String,
  channel_name: String,
  team_id: String,
  team_domain: String,
  post_id: String,
  trigger_id: String,
  data_source: String,
  `type`: String,
  context: Json
) derives Codec.AsObject

sealed trait AppResponse derives Codec.AsObject

object AppResponse {
  case class Ok() extends AppResponse

}

class Mattermost[F[_]: Monad](val ctx: AppContext[F]) {

  type CommandActions = (AppContext[F], CommandContext) => F[AppResponse]

  type DialogActions = (AppContext[F], DialogContext) => F[AppResponse]

  type EventActions = (AppContext[F], ActionContext) => F[AppResponse]

  private val commands: mutable.Map[String, CommandActions] = mutable.Map.empty

  private val dialogs: mutable.Map[String, DialogActions] = mutable.Map.empty

  private val events: mutable.Map[String, EventActions] = mutable.Map.empty

  def command(name: String)(action: CommandActions): Mattermost[F] =
    commands += name -> action
    this

  def dialog(name: String)(action: DialogActions): Mattermost[F] =
    dialogs += name -> action
    this

  def actions(name: String)(action: EventActions): Mattermost[F] = {
    events += name -> action
    this
  }

  def handleCommand(name: String, commandCtx: CommandContext): F[AppResponse] =
    commands.get(name) match
      case Some(action) => action(ctx, commandCtx)
      case None         => AppResponse.Ok().pure[F]

  def handleAction(name: String, actionCtx: ActionContext): F[AppResponse] =
    events.get(name) match
      case Some(action) => action(ctx, actionCtx)
      case None         => AppResponse.Ok().pure[F]

  def handleDialog(name: String, dialogCtx: DialogContext): F[AppResponse] =
    dialogs.get(name) match
      case Some(action) => action(ctx, dialogCtx)
      case None         => AppResponse.Ok().pure[F]

}

object Mattermost {}

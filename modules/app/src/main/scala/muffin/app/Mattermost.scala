package muffin.app

import cats.{Applicative, Monad}
import io.circe.{
  Codec,
  Decoder,
  DecodingFailure,
  Encoder,
  FailedCursor,
  HCursor,
  Json
}
import muffin.predef.*

import scala.collection.mutable
import scala.reflect.ClassTag
import cats.syntax.all.given
import io.circe.Decoder.Result
import muffin.app
import muffin.posts.Attachment

import scala.quoted.*
import io.circe.syntax.*

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

sealed trait DialogSubmissionValue

object DialogSubmissionValue {
  case class Str(value: String) extends DialogSubmissionValue
  case class Num(value: Long) extends DialogSubmissionValue
  case class Bool(value: Boolean) extends DialogSubmissionValue

  given Decoder[DialogSubmissionValue] = (c: HCursor) =>
    c.value.asNumber
      .flatMap(_.toLong.map(Num(_)))
      .orElse(c.value.asBoolean.map(Bool(_)))
      .orElse(c.value.asString.map(Str(_)))
      .toRight(DecodingFailure("Can't decode submission", c.history))

  given Encoder[DialogSubmissionValue] =
    case Str(value)  => Json.fromString(value)
    case Num(value)  => Json.fromLong(value)
    case Bool(value) => Json.fromBoolean(value)
}

trait DialogSubmissionExtractor[U] {
  def get(submission: DialogSubmissionValue): Option[U]
}

object DialogSubmissionExtractor {
  def partial[T](submission: DialogSubmissionValue)(
    fun: PartialFunction[DialogSubmissionValue, Option[T]]
  ): Option[T] = fun.applyOrElse(submission, _ => None)

  given DialogSubmissionExtractor[Long] = partial(_) {
    case DialogSubmissionValue.Num(value) =>
      value.some
  }

  given DialogSubmissionExtractor[String] = partial(_) {
    case DialogSubmissionValue.Str(value) =>
      value.some
  }

  given DialogSubmissionExtractor[Boolean] = partial(_) {
    case DialogSubmissionValue.Bool(value) =>
      value.some
  }

  given DialogSubmissionExtractor[Int] = partial(_) {
    case DialogSubmissionValue.Num(value) =>
      value.toInt.some
  }
}

case class DialogContext(
  callback_id: String,
  state: String,
  user_id: UserId,
  channel_id: ChannelId,
  team_id: String,
  submission: Map[String, DialogSubmissionValue],
  cancelled: Boolean
) derives Codec.AsObject {
  def submission[T: DialogSubmissionExtractor](key: String): Option[T] =
    submission.get(key).flatMap(summon[DialogSubmissionExtractor[T]].get(_))
}

case class ActionContext(
  user_id: UserId,
  user_name: Login,
  channel_id: ChannelId,
  channel_name: String,
  team_id: String,
  team_domain: String,
  post_id: MessageId,
  trigger_id: String,
  data_source: String,
  `type`: String,
  context: Json
) derives Codec.AsObject

enum ResponseType:
  case Ephemeral, InChannel

object ResponseType {
  given encoder: Encoder[ResponseType] = {
    case Ephemeral => Encoder.encodeString("ephemeral")
    case InChannel => Encoder.encodeString("in_channel")
  }
}

sealed trait AppResponse

object AppResponse {
  case class Ok() extends AppResponse derives Codec.AsObject

  case class Message(
    text: String,
    response_type: String,
    attachments: List[Attachment]
  ) extends AppResponse
      derives Codec.AsObject

  given Encoder[AppResponse] =
    case value: Ok      => value.asJson
    case value: Message => value.asJson

  given Decoder[AppResponse] = new Decoder[AppResponse]:
    override def apply(c: HCursor): Result[AppResponse] =
      c.downField("text") match
        case cursor: FailedCursor =>  Right(AppResponse.Ok())
        case _                    => Right(AppResponse.Message("", "", Nil))
}

class Mattermost[F[_]: Monad](val ctx: AppContext[F], serviceUrl: String) {

  type CommandActions = (AppContext[F], CommandContext) => F[AppResponse]

  type DialogActions = (AppContext[F], DialogContext) => F[AppResponse]

  type EventActions = (AppContext[F], ActionContext) => F[AppResponse]

  val commands: mutable.Map[String, CommandActions] = mutable.Map.empty

  val dialogs: mutable.Map[String, DialogActions] = mutable.Map.empty

  val events: mutable.Map[String, EventActions] = mutable.Map.empty

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

  def command(name: String): String =
    serviceUrl + s"/commands/$name"

  def dialog(name: String): String =
    serviceUrl + s"/dialogs/$name"

  def actions(name: String): String =
    serviceUrl + s"/actions/$name"

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

package muffin.input

import cats.arrow.FunctionK
import cats.syntax.all.given
import cats.{MonadThrow, ~>}
import io.circe.*
import io.circe.Decoder.Result
import io.circe.syntax.given
import muffin.api.posts.Attachment
import muffin.codec.{Decode, RawDecode}
import muffin.predef.*

enum ResponseType:
  case Ephemeral, InChannel

object ResponseType {
  given encoder: Encoder[ResponseType] = {
    case Ephemeral => Encoder.encodeString("ephemeral")
    case InChannel => Encoder.encodeString("in_channel")
  }

  given decoder: Decoder[ResponseType] = Decoder.decodeString.emap {
    case "ephemeral"  => Right(ResponseType.Ephemeral)
    case "in_channel" => Right(ResponseType.InChannel)
    case _            => Left("Invalid response type")
  }
}

sealed trait AppResponse

object AppResponse {
  case class Ok() extends AppResponse derives Codec.AsObject

  case class Message(
    text: String,
    response_type: ResponseType,
    attachments: List[Attachment]
  ) extends AppResponse
      derives Codec.AsObject

  given Encoder[AppResponse] =
    case value: Ok      => value.asJson
    case value: Message => value.asJson

  given Decoder[AppResponse] = (c: HCursor) =>
    c.downField("text") match
      case _: FailedCursor => Right(AppResponse.Ok())
      case _               => c.as[Message]
}

case class RawAction[R](
  user_id: UserId,
  user_name: Login,
  channel_id: ChannelId,
  channel_name: String,
  team_id: TeamId,
  team_domain: String,
  post_id: MessageId,
  trigger_id: String,
  data_source: String,
  `type`: String,
  context: R
) {
  def asTyped[F[_], T](implicit
    monad: MonadThrow[F],
    decoder: RawDecode[R, T]
  ): F[Action[T]] = {
    MonadThrow[F]
      .fromEither(decoder(context))
      .map(action =>
        Action(
          user_id,
          user_name,
          channel_id,
          channel_name,
          team_id,
          team_domain,
          post_id,
          trigger_id,
          data_source,
          `type`,
          action
        )
      )
  }
}
//
//case class RawDialogAction(
//  callback_id: String,
//  state: String,
//  user_id: UserId,
//  channel_id: ChannelId,
//  team_id: String,
//  cancelled: Boolean,
//  submission: Json
//) derives Codec.AsObject
//
//
//case class DialogAction[T](
//  callback_id: String,
//  state: String,
//  user_id: UserId,
//  channel_id: ChannelId,
//  team_id: String,
//  cancelled: Boolean,
//  submission: T
//)
//


case class Action[T](
  userId: UserId,
  userName: Login,
  channelId: ChannelId,
  channelName: String,
  teamId: TeamId,
  teamDomain: String,
  postId: MessageId,
  triggerId: String,
  dataSource: String,
  `type`: String,
  context: T
) derives Codec.AsObject

case class CommandContext(
  channelId: ChannelId,
  channelName: String,
  responseUrl: String, // TODO URL
  teamDomain: String,
  teamId: TeamId,
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

  given Decoder[Option[DialogSubmissionValue]] = (c: HCursor) =>
    Right(
      c.value.asNumber
        .flatMap(_.toLong.map(Num(_)))
        .orElse(c.value.asBoolean.map(Bool(_)))
        .orElse(c.value.asString.map(Str(_)))
    )

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
    case DialogSubmissionValue.Str(value) if value.toIntOption.isDefined =>
      value.toIntOption
  }
}

case class DialogContext(
  callback_id: String,
  state: String,
  user_id: UserId,
  channel_id: ChannelId,
  team_id: String,
  submission: Map[String, Option[DialogSubmissionValue]],
  cancelled: Boolean
) derives Codec.AsObject {
  def submission[T: DialogSubmissionExtractor](key: String): Option[T] =
    submission.get(key).flatten.flatMap(summon[DialogSubmissionExtractor[T]].get(_))
}

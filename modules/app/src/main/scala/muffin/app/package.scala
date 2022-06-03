package muffin.app

import muffin.predef.*

import io.circe.Json
import io.circe.Decoder.Result
import io.circe.DecodingFailure
import io.circe.{Codec, Decoder, Encoder, FailedCursor, HCursor}
import io.circe.syntax.given
import muffin.posts.Attachment

import cats.MonadThrow
import cats.syntax.all.given

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

case class RawAction(
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
) derives Codec.AsObject {
  def asTyped[F[_], T](name: String)(implicit
    monad: MonadThrow[F],
    decoder: Decoder[T]
  ): F[(String, Action[T])] =
    MonadThrow[F]
      .fromEither(context.as[T])
      .map(action =>
        (
          name,
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
      )
}

case class Action[T](
  userId: UserId,
  userName: Login,
  channelId: ChannelId,
  channelName: String,
  teamId: String,
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

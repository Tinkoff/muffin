package muffin.input

import cats.arrow.FunctionK
import cats.syntax.all.given
import cats.{MonadThrow, ~>}
import muffin.api.posts.Attachment
import muffin.codec.{Decode, RawDecode}
import muffin.predef.*

enum ResponseType:
  case Ephemeral, InChannel

sealed trait AppResponse

object AppResponse {
  case class Ok() extends AppResponse

  case class Message(
    text: String,
    responseType: ResponseType,
    attachments: List[Attachment] = Nil
  ) extends AppResponse

  case class Errors(errors: Map[String, String]) extends AppResponse
}

case class RawAction[R](
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
  context: Option[R]
) {
  def asTyped[F[_], T](implicit
    monad: MonadThrow[F],
    decoder: RawDecode[R, T]
  ): F[MessageAction[T]] = {
    MonadThrow[F]
      .fromEither(context.fold(Left(new Exception("can't convert from None")))(decoder(_)))
      .map(action =>
        MessageAction(
          userId,
          userName,
          channelId,
          channelName,
          teamId,
          teamDomain,
          postId,
          triggerId,
          dataSource,
          `type`,
          action
        )
      )
  }
}

case class MessageAction[T](
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
)

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
  callbackId: String,
  state: String,
  userId: UserId,
  channelId: ChannelId,
  teamId: TeamId,
  submission: Map[String, Option[DialogSubmissionValue]],
  cancelled: Boolean
) {
  def submission[T: DialogSubmissionExtractor](key: String): Option[T] =
    submission.get(key).flatten.flatMap(summon[DialogSubmissionExtractor[T]].get(_))
}

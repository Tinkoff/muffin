package muffin.model

import cats.syntax.all.given

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

case class CommandAction(
    channelId: ChannelId,
    channelName: String,
    responseUrl: String,
    teamDomain: String,
    teamId: TeamId,
    text: Option[String],
    triggerId: String,
    userId: UserId,
    userName: String
)

trait DialogSubmission[T] {
  def get(submission: String): Option[T]
}

object DialogSubmission {

  def partial[T](submission: String)(fun: PartialFunction[String, Option[T]]): Option[T] =
    fun.applyOrElse(
      submission,
      _ => None
    )

  given DialogSubmission[Long] = partial(_)(_.toLongOption)
  given DialogSubmission[String] = partial(_)(_.some)
  given DialogSubmission[Boolean] = partial(_)(_.toBooleanOption)
  given DialogSubmission[Int] = partial(_)(_.toIntOption)

}

case class DialogAction[T](
    callbackId: Option[String],
    state: T,
    userId: UserId,
    channelId: ChannelId,
    teamId: TeamId,
    submission: Map[String, String],
    cancelled: Boolean
) {

  def submission[T](key: String)(using ds: DialogSubmission[T]): Option[T] = submission.get(key).flatMap(ds.get)

}

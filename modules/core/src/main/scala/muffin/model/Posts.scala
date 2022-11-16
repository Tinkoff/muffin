package muffin.model

import scala.collection.immutable.List

import cats.syntax.all.given
import fs2.Stream

import muffin.*
import muffin.api.*

case class Post[T](
    id: MessageId
    //  create_at: Long,
    //  update_at: Long,
    //  delete_at: Long,
    //  edit_at: Long,
    //  user_id: UserId,
    //  channel_id: ChannelId,
    //  root_id: MessageId,
    //  original_id: MessageId,
    //  message: String,
    //  `type`: String,
    //    props: Option[Props[T]] = None
    //  hashtag: Option[String],
    //  file_ids: List[String],
    //  pending_post_id: Option[String],
    //  metadata: PostMetadata
)

case class PostMetadata(reactions: Option[ReactionInfo])

case class Props[T](attachments: List[Attachment[T]] = Nil)

object Props {
  def empty: Props[Nothing] = Props(Nil)
}

case class Attachment[+T](
    fallback: Option[String] = None,
    color: Option[String] = None,
    pretext: Option[String] = None,
    text: Option[String] = None,
    authorName: Option[String] = None,
    authorLink: Option[String] = None,
    authorIcon: Option[String] = None,
    title: Option[String] = None,
    titleLink: Option[String] = None,
    fields: List[AttachmentField] = Nil,
    imageUrl: Option[String] = None,
    thumbUrl: Option[String] = None,
    footer: Option[String] = None,
    footerIcon: Option[String] = None,
    actions: List[Action[T]] = Nil
)

case class AttachmentField(title: String, value: String, short: Boolean = false)

sealed trait Action[+T] {
  val id: String

  val name: String

  val integration: Integration[T]
}

object Action {

  case class Button[+T](id: String, name: String, integration: Integration[T], style: Style = Style.Default)
    extends Action[T]

  case class Select[+T](
      id: String,
      name: String,
      integration: Integration[T],
      options: List[SelectOption] = Nil,
      dataSource: Option[DataSource] = None
  ) extends Action[T]

}

enum Integration[+T] {
  case Url(url: String) extends Integration[Nothing]
  case Context[T](url: String, ctx: T) extends Integration[T]
}

enum Style {
  case Good
  case Warning
  case Danger
  case Default
  case Primary
  case Success
}

enum DataSource {
  case Channels
  case Users
}

case class SelectOption(text: String, value: String)

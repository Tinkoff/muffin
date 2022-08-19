package muffin.api.posts

import muffin.api.reactions.ReactionInfo
import muffin.predef.*
import cats.syntax.all.given

import java.time.LocalDateTime

case class Post(
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
                 //  props: Option[Props] = None,
                 //  hashtag: Option[String],
                 //  file_ids: List[String],
                 //  pending_post_id: Option[String],
                 //  metadata: PostMetadata
               )

case class PostMetadata(reactions: Option[ReactionInfo])

case class Props(attachments: List[Attachment] = Nil)

case class Attachment(
                       fallback: Option[String],
                       color: Option[String], // TODO Make Id
                       pretext: Option[String],
                       text: Option[String],
                       authorName: Option[String],
                       authorLink: Option[String], // TODO Make URL
                       authorIcon: Option[String], // TODO Make URL

                       title: Option[String],
                       titleLink: Option[String], // TODO Make URL

                       fields: List[AttachmentField],
                       imageUrl: Option[String], // TODO Make URL
                       thumbUrl: Option[String], // TODO Make URL

                       footer: Option[String],
                       footerIcon: Option[String], // TODO Make URL

                       actions: List[Action]
                     )
object Attachment {
  def apply(
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

    actions: List[Action] = Nil
  ): Attachment =
    Attachment(
      fallback = text,
      color,
      pretext,
      text,
      authorName,
      authorLink,
      authorIcon,
      title,
      titleLink,
      fields,
      imageUrl,
      thumbUrl,
      footer,
      footerIcon,
      actions
    )
}

case class AttachmentField(title: String, value: String, short: Boolean = false)

sealed trait Action

object Action {
  case class Button(
                     id: String,
                     name: String,
                     integration: Integration,
                     style: Option[Style] = None,
                   ) extends Action

  case class Select(
                     id: String,
                     name: String,
                     integration: Integration,
                     options: List[SelectOption] = Nil,
                     dataSource: Option[DataSource] = None
                   ) extends Action

}

case class Integration(url: String, context: Option[String] = None)

object Integration{
  def apply(url: String, context: String): Integration = Integration(url, context.some)
}

enum Style:
  case Good, Warning, Danger, Default, Primary, Success

enum DataSource:
  case Channels, Users

case class SelectOption(text: String, value: String)

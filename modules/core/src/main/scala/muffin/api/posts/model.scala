package muffin.api.posts

import io.circe.Decoder.Result
import muffin.api.reactions.ReactionInfo
import muffin.predef.*

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
                       fallback: Option[String] = None,
                       color: Option[String] = None, // TODO Make Id
                       pretext: Option[String] = None,
                       text: Option[String] = None,
                       authorName: Option[String] = None,
                       authorLink: Option[String] = None, // TODO Make URL
                       authorIcon: Option[String] = None, // TODO Make URL

                       title: Option[String] = None,
                       titleLink: Option[String] = None, // TODO Make URL

                       fields: List[AttachmentField] = Nil,
                       imageUrl: Option[String] = None, // TODO Make URL
                       thumbUrl: Option[String] = None, // TODO Make URL

                       footer: Option[String] = None,
                       footerIcon: Option[String] = None, // TODO Make URL

                       actions: List[Action] = Nil
                     )

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


case class Integration(url: String, context: String)

enum Style:
  case Good, Warning, Danger, Default, Primary, Success

enum DataSource:
  case Channels, Users

case class SelectOption(text: String, value: String)


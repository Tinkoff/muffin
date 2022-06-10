package muffin.posts

import io.circe.Decoder.Result
import io.circe.{Codec, Decoder, Encoder, HCursor, Json, JsonObject}
import io.circe.syntax.given
import muffin.predef.*
import muffin.reactions.ReactionInfo

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
               ) derives Codec.AsObject

case class PostMetadata(reactions: Option[ReactionInfo]) derives Codec.AsObject

case class CreatePostRequest(
                              channel_id: ChannelId,
                              message: Option[String] = None,
                              props: Option[Props] = None,
                              root_id: Option[MessageId] = None,
                              file_ids: List[String] = Nil // TODO make Id
                            ) derives Encoder.AsObject

type CreatePostResponse = Post

case class CreatePostEphemeral(user_id: UserId, post: CreatePostRequest)
  derives Encoder.AsObject

opaque type GetPostRequest = MessageId

case class GetPostResponse(message: String) derives Codec.AsObject

type DeletePostRequest = MessageId
type DeletePostResponse = Unit

case class Props(attachments: List[Attachment] = Nil) derives Encoder.AsObject

case class Attachment(
                       fallback: String,
                       color: Option[String] = None, // TODO Make Id
                       pretext: Option[String] = None,
                       text: Option[String] = None,
                       author_name: Option[String] = None,
                       author_link: Option[String] = None, // TODO Make URL
                       author_icon: Option[String] = None, // TODO Make URL

                       title: Option[String] = None,
                       title_link: Option[String] = None, // TODO Make URL

                       fields: List[AttachmentField] = Nil,
                       imageUrl: Option[String] = None, // TODO Make URL
                       thumb_url: Option[String] = None, // TODO Make URL

                       footer: Option[String] = None,
                       footer_icon: Option[String] = None, // TODO Make URL

                       actions: List[MessageAction] = Nil
                     ) derives Codec.AsObject

case class AttachmentField(title: String, value: String, short: Boolean = false)
  derives Codec.AsObject

sealed trait MessageAction

case class Button(
                   id: String,
                   name: String,
                   integration: Integration,
                   style: Option[Style] = None,
                 ) extends MessageAction derives Codec.AsObject

case class Select(
                   id: String,
                   name: String,
                   integration: Integration,
                   options: List[SelectOption] = Nil,
                   dataSource: Option[DataSource] = None
                 ) extends MessageAction derives Codec.AsObject

object MessageAction {
  given Encoder[MessageAction] = {
    case value: Button => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("button"))).dropNullValues
    case value: Select => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("select"))).dropNullValues
  }

  given Decoder[MessageAction] = (c: HCursor) =>
    c.downField("type").as[String].flatMap {
      case "button" => c.as[Button]
      case "select" => c.as[Button]
    }
}

case class Integration(url: String, context: String) derives Codec.AsObject

enum Style:
  case Good, Warning, Danger, Default, Primary, Success

object Style {
  given Encoder[Style] = (s: Style) => Json.fromString(s.toString.toLowerCase)

  given Decoder[Style] = (c: HCursor) => c.as[String].map(s => Style.valueOf(s))
}

enum DataSource derives Codec.AsObject :
  case Channels, Users

case class SelectOption(text: String, value: String) derives Codec.AsObject

type PinPostRequest = MessageId
type PinPostResponse = Boolean

type UnpinPostRequest = MessageId
type UnpinPostResponse = Boolean

case class PerformActionRequest(post_id: MessageId, action_id: String)

type PerformActionResponse = Boolean

type GetPostsByIdsRequest = List[MessageId]
type GetPostsByIdsResponse = List[Unit] //TODO

case class PatchPostRequest(
  post_id: MessageId,
  is_pinned: Option[Boolean] = None,
  message: Option[String] = None,
  props: Option[Props] = None,
  root_id: Option[MessageId] = None,
  file_ids: Option[List[String]] = None // TODO make Id
) derives Encoder.AsObject


enum SearchPostModifiers:
  case Since(since: LocalDateTime)
  case BeforeAfter(beforeId: Option[MessageId], afterId: Option[MessageId])

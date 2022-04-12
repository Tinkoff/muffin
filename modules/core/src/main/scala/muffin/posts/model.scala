package muffin.posts

import io.circe.{Codec, Decoder, Encoder, Json, JsonObject}
import muffin.predef.*
import muffin.reactions.ReactionInfo

case class Post(
  id: MessageId,
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
  message: String,
  props: Option[Props] = None,
  root_id: Option[MessageId] = None,
  file_ids: List[String] = Nil // TODO make Id
) derives Encoder.AsObject

type CreatePostResponse = Post

case class CreatePostEphemeral(user_id: UserId, post: CreatePostRequest)
    derives Encoder.AsObject

opaque type GetPostRequest = MessageId

case class GetPostResponse(message: String) derives Codec.AsObject

opaque type DeletePostRequest = MessageId
opaque type DeletePostResponse = Boolean

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

  actions: List[Button] = Nil
) derives Encoder.AsObject

case class AttachmentField(title: String, value: String, short: Boolean = false)
    derives Encoder.AsObject

//sealed trait MessageAction  derives Encoder.AsObject

case class Button(
  id: String,
  name: String,
  integration: Integration,
  style: Option[Style] = None
) derives Encoder.AsObject // extends MessageAction

//case class Select(
//    id:String,
//    name:String,
//    integration: Integration,
//    options: List[SelectOption] = Nil,
//    dataSource: Option[DataSource] = None
//                 )extends MessageAction

case class Integration(url: String, context: JsonObject) {
  def access[T: Decoder]: Decoder.Result[T] = Json.fromJsonObject(context).as[T]
}

object Integration {

  given encoder: Encoder[Integration] = obj =>
    Json.obj(
      "url" -> Json.fromString(obj.url),
      "context" -> Json.fromJsonObject(obj.context)
    )
}

enum Style derives Encoder.AsObject:
  case Good, Warning, Danger, Default, Primary, Success

enum DataSource derives Encoder.AsObject:
  case Channels, Users

case class SelectOption(text: String, value: String) derives Encoder.AsObject

type PinPostRequest = MessageId
type PinPostResponse = Boolean

type UnpinPostRequest = MessageId
type UnpinPostResponse = Boolean

case class PerformActionRequest(post_id: MessageId, action_id: String)
type PerformActionResponse = Boolean

type GetPostsByIdsRequest = List[MessageId]
type GetPostsByIdsResponse = List[Unit] //TODO

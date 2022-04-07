package muffin.posts

import io.circe.{Codec, Decoder, Encoder, Json, JsonObject}
import muffin.predef.*

case class CreatePostRequest(
  channel_id: ChannelId,
  message: String,
  props: Option[Props]
  //    root_id:Option[MessageId] = None,
  //    file_ids: List[String] = Nil // TODO make Id
) derives Encoder.AsObject

case class CreatePostResponse(id: MessageId) derives Codec.AsObject

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

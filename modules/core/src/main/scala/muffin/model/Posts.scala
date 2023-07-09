package muffin.model

import scala.collection.immutable.List

import cats.syntax.all.given

import muffin.*
import muffin.codec.*

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
    message: String,
    //  `type`: String,
    props: Props = Props.empty
    //  hashtag: Option[String],
    //  file_ids: List[String],
    //  pending_post_id: Option[String],
//      metadata: PostMetadata
)

case class PostMetadata(reactions: Option[ReactionInfo])

case class Props(attachments: List[Attachment] = Nil)

object Props {
  def empty: Props = Props(Nil)
}

case class Attachment(
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
    actions: List[Action] = Nil
)

case class AttachmentField(title: String, value: String, short: Boolean = false)

sealed trait Action {
  val id: String
  val name: String

  def integrationUrl: String
  def integrationContext[T: Decode]: Option[T]
}

object Action {

  case class Button private[muffin] (
      id: String,
      name: String,
      style: Style = Style.Default
  )(private[muffin] val raw: RawIntegration)
    extends Action {

    def integrationUrl: String = raw.url

    def integrationContext[T: Decode]: Option[T] = raw.ctx.flatMap(Decode[T].apply(_).toOption)

    def setIntegration[T: Encode](integration: Integration[T]): Button =
      Button(id, name, style)(integration match {
        case Integration.Url(url)          => RawIntegration(url, None)
        case Integration.Context(url, ctx) => RawIntegration(url, Encode[T].apply(ctx).some)
      })

  }

  case class Select private[muffin] (
      id: String,
      name: String,
      options: List[SelectOption] = Nil,
      dataSource: Option[DataSource] = None
  )(private[muffin] val raw: RawIntegration)
    extends Action {

    def integrationUrl: String = raw.url

    def integrationContext[T: Decode]: Option[T] = raw.ctx.flatMap(Decode[T].apply(_).toOption)

    def setIntegration[T: Encode](integration: Integration[T]): Select =
      Select(id, name, options, dataSource)(integration match {
        case Integration.Url(url)          => RawIntegration(url, None)
        case Integration.Context(url, ctx) => RawIntegration(url, Encode[T].apply(ctx).some)
      })

  }

}

enum Integration[+T] {
  case Url(url: String) extends Integration[Nothing]
  case Context[A](url: String, ctx: A) extends Integration[A]
}

private[muffin] case class RawIntegration(url: String, ctx: Option[String])

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

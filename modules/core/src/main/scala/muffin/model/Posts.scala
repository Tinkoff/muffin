package muffin.model

import scala.collection.immutable.List

import cats.syntax.all.given
import fs2.Stream

import muffin.*
import muffin.api.*
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

  class Button private[muffin] (
      val id: String,
      val name: String,
      val style: Style = Style.Default
  )(private[muffin] val raw: RawIntegration)
    extends Action {

    def integrationUrl: String =
      raw match {
        case RawIntegration.Url(url)        => url
        case RawIntegration.Context(url, _) => url
      }

    def integrationContext[T: Decode]: Option[T] =
      raw match {
        case RawIntegration.Url(_)          => None
        case RawIntegration.Context(_, ctx) => Decode[T].apply(ctx).toOption
      }

    def copy(id: String = this.id, name: String = this.name, style: Style = this.style): Button =
      new Button(id, name, style)(raw)

    def copyIntegration[T: Encode](integration: Integration[T]): Button =
      new Button(id, name, style)(integration match {
        case Integration.Url(url)          => RawIntegration.Url(url)
        case Integration.Context(url, ctx) => RawIntegration.Context(url, Encode[T].apply(ctx))
      })

    override def toString = s"Action.Button(id: $id, name: $name, style: $style, integration: $raw)"
  }

  class Select private[muffin] (
      val id: String,
      val name: String,
      val options: List[SelectOption] = Nil,
      val dataSource: Option[DataSource] = None
  )(private[muffin] val raw: RawIntegration)
    extends Action {

    def integrationUrl: String =
      raw match {
        case RawIntegration.Url(url)        => url
        case RawIntegration.Context(url, _) => url
      }

    def integrationContext[T: Decode]: Option[T] =
      raw match {
        case RawIntegration.Url(_)          => None
        case RawIntegration.Context(_, ctx) => Decode[T].apply(ctx).toOption
      }

    def copy(
        id: String = this.id,
        name: String = this.name,
        options: List[SelectOption] = this.options,
        dataSource: Option[DataSource] = this.dataSource
    ): Select = new Select(id, name, options, dataSource)(raw)

    def copyIntegration[T: Encode](integration: Integration[T]): Select =
      new Select(id, name, options, dataSource)(integration match {
        case Integration.Url(url)          => RawIntegration.Url(url)
        case Integration.Context(url, ctx) => RawIntegration.Context(url, Encode[T].apply(ctx))
      })

    override def toString =
      s"Action.Select(id: $id, name: $name, options: $options, dataSource: $dataSource, integration: $raw)"

  }

}

enum Integration[+T] {
  case Url(url: String) extends Integration[Nothing]
  case Context[T](url: String, ctx: T) extends Integration[T]
}

private[muffin] enum RawIntegration(url: String) {
  case Url(url: String) extends RawIntegration(url)
  case Context(url: String, ctx: String) extends RawIntegration(url)
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

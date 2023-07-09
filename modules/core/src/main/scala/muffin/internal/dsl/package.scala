package muffin.internal.dsl

import cats.MonadThrow
import cats.syntax.all.given

import muffin.api.*
import muffin.codec.Encode
import muffin.internal.macros.RouterMacro
import muffin.internal.router.*
import muffin.model.*
import muffin.router.*

trait AppResponseSyntax {

  def ok: AppResponse = AppResponse.Ok()

  def errors(errs: Map[String, String]): AppResponse = AppResponse.Errors(errs)

  def errors(errs: (String, String)*): AppResponse = errors(errs.toMap)

  def ephemeral: AppResponseMessageQuery = AppResponseMessageQuery.ephemeral

  def inChannel: AppResponseMessageQuery = AppResponseMessageQuery.inChannel

  class AppResponseMessageQuery private (message: AppResponse.Message) {

    def text(text: String): AppResponseMessageQuery = AppResponseMessageQuery(message.copy(text = text.some))

    def attachments(attachments: List[Attachment]): AppResponseMessageQuery =
      AppResponseMessageQuery(
        message.copy(attachments = attachments)
      )

    def make: AppResponse = message

  }

  object AppResponseMessageQuery {

    def ephemeral: AppResponseMessageQuery =
      AppResponseMessageQuery(AppResponse.Message(responseType = ResponseType.Ephemeral))

    def inChannel: AppResponseMessageQuery =
      AppResponseMessageQuery(AppResponse.Message(responseType = ResponseType.InChannel))

  }

}

trait MessageSyntax {

  def button[T: Encode](
      name: String,
      integration: Integration.type => Integration[T],
      style: Style = Style.Default,
      id: Option[String] = None
  ): Action.Button =
    Action.Button(id.getOrElse(name), name, style)(integration(Integration) match {
      case Integration.Url(url)          => RawIntegration(url, None)
      case Integration.Context(url, ctx) => RawIntegration(url, Encode[T].apply(ctx).some)
    })

  def selectOptions[T: Encode](
      name: String,
      integration: Integration.type => Integration[T],
      options: List[SelectOption],
      id: Option[String] = None
  ): Action.Select =
    Action.Select(id.getOrElse(name), name, options)(integration(Integration) match {
      case Integration.Url(url)          => RawIntegration(url, None)
      case Integration.Context(url, ctx) => RawIntegration(url, Encode[T].apply(ctx).some)
    })

  def selectSource[T: Encode](
      name: String,
      integration: Integration.type => Integration[T],
      source: DataSource.type => DataSource,
      id: Option[String] = None
  ): Action.Select =
    Action.Select(id.getOrElse(name), name, dataSource = source(DataSource).some)(integration(Integration) match {
      case Integration.Url(url)          => RawIntegration(url, None)
      case Integration.Context(url, ctx) => RawIntegration(url, Encode[T].apply(ctx).some)
    })

  def attachment: AttachmentQuery = AttachmentQuery()

  class AttachmentQuery private (attachment: Attachment) {

    def title(title: String, link: Option[String]): AttachmentQuery =
      new AttachmentQuery(attachment.copy(title = title.some, titleLink = link))

    def footer(footer: String, icon: Option[String]): AttachmentQuery =
      new AttachmentQuery(attachment.copy(footer = footer.some, footerIcon = icon))

    def author(name: String, link: Option[String], icon: Option[String]): AttachmentQuery =
      new AttachmentQuery(attachment.copy(authorName = name.some, authorLink = link, authorIcon = icon))

    def color(color: String): AttachmentQuery = new AttachmentQuery(attachment.copy(color = color.some))

    def text(text: String, pretext: Option[String] = None, fallback: Option[String] = None): AttachmentQuery =
      new AttachmentQuery(attachment.copy(text = text.some, pretext = pretext, fallback = fallback.orElse(text.some)))

    def image(img: String): AttachmentQuery = new AttachmentQuery(attachment.copy(imageUrl = img.some, thumbUrl = None))

    def thumb(img: String): AttachmentQuery = new AttachmentQuery(attachment.copy(thumbUrl = img.some, imageUrl = None))

    def fields(fields: List[AttachmentField]): AttachmentQuery = new AttachmentQuery(attachment.copy(fields = fields))

    def action(action: Action): AttachmentQuery =
      new AttachmentQuery(attachment.copy(actions = action :: attachment.actions))

    def make: Attachment = attachment.copy(actions = attachment.actions.reverse)
  }

  object AttachmentQuery {
    private[muffin] def apply(): AttachmentQuery = new AttachmentQuery(Attachment())
  }

}

trait DialogSyntax {

  def dialog(title: String)(using Encode[Unit]): DialogQuery = DialogQuery(title, ())

  def dialog[T: Encode](title: String, state: T): DialogQuery = DialogQuery(title, state)

  class DialogQuery private (d: Dialog) {

    def callback(id: String): DialogQuery = new DialogQuery(d.copy(callbackId = id.some)(d.state))

    def introduction(text: String): DialogQuery = new DialogQuery(d.copy(introductionText = text.some)(d.state))

    def submitLabel(label: String): DialogQuery = new DialogQuery(d.copy(submitLabel = label.some)(d.state))

    def notifyOnCancel: DialogQuery = new DialogQuery(d.copy(notifyOnCancel = true)(d.state))

    def element(element: Element): DialogQuery = new DialogQuery(d.copy(elements = element :: d.elements)(d.state))

    def make: Dialog = d.copy(elements = d.elements.reverse)(d.state)
  }

  object DialogQuery {

    private[muffin] def apply[T: Encode](title: String, state: T): DialogQuery =
      new DialogQuery(Dialog(title = title)(Encode[T].apply(state)))

  }

}

trait RouterSyntax {

  inline def handle[F[_], H, N <: Singleton](
      inline handle: H,
      name: N
  ): Handle[F, H, N, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple] =
    Handle[F, H, N, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple](
      handle
    )

  extension [L <: RouterDSL](left: L) {

    def <~>[R <: RouterDSL](right: R) = <>(left, right)

    inline def in[F[_], G[_]](
        onDialogMissing: HttpAction => AppResponse = _ => AppResponse.Ok(),
        onActionMissing: HttpAction => AppResponse = _ => AppResponse.Ok(),
        onCommandMissing: CommandAction => AppResponse = _ => AppResponse.Ok()
    )(using syncF: MonadThrow[F], syncG: MonadThrow[G]): G[Router[F]] =
      ${
        RouterMacro
          .router[F, G, L]('left, 'syncF, 'syncG, 'onDialogMissing, 'onActionMissing, 'onCommandMissing)
      }

  }

}

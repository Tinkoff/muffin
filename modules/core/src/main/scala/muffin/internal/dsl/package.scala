package muffin.internal.dsl

import cats.MonadThrow
import cats.syntax.all.given

import muffin.api.*
import muffin.internal.macros.RouterMacro
import muffin.internal.router.*
import muffin.model.*
import muffin.router.*

trait AppResponseSyntax {

  def ok: AppResponse[Nothing] = AppResponse.Ok()

  def errors(errs: Map[String, String]): AppResponse[Nothing] = AppResponse.Errors(errs)

  def errors(errs: (String, String)*): AppResponse[Nothing] = errors(errs.toMap)

  def ephemeral: AppResponseMessageQuery[Nothing] = AppResponseMessageQuery.ephemeral

  def inChannel: AppResponseMessageQuery[Nothing] = AppResponseMessageQuery.inChannel

  class AppResponseMessageQuery[T] private (message: AppResponse.Message[T]) {

    def text(text: String): AppResponseMessageQuery[T] = AppResponseMessageQuery(message.copy(text = text.some))

    def attachments[X >: T](attachments: List[Attachment[X]]): AppResponseMessageQuery[X] =
      AppResponseMessageQuery(
        message.copy(attachments = attachments)
      )

    def make: AppResponse[T] = message

  }

  object AppResponseMessageQuery {

    def ephemeral: AppResponseMessageQuery[Nothing] =
      AppResponseMessageQuery[Nothing](AppResponse.Message(responseType = ResponseType.Ephemeral))

    def inChannel: AppResponseMessageQuery[Nothing] =
      AppResponseMessageQuery[Nothing](AppResponse.Message(responseType = ResponseType.InChannel))

  }

}

trait MessageSyntax {

  def actionButton[T](
      name: String,
      integration: Integration.type => Integration[T],
      style: Style = Style.Default,
      id: Option[String] = None
  ): Action[T] = Action.Button(id.getOrElse(name), name, integration(Integration), style)

  def actionSelectOptions[T](
      name: String,
      integration: Integration.type => Integration[T],
      options: List[SelectOption],
      id: Option[String] = None
  ): Action[T] = Action.Select(id.getOrElse(name), name, integration(Integration), options)

  def actionSelectSource[T](
      name: String,
      integration: Integration.type => Integration[T],
      source: DataSource.type => DataSource,
      id: Option[String] = None
  ): Action[T] = Action.Select(id.getOrElse(name), name, integration(Integration), dataSource = source(DataSource).some)

  def attachment: AttachmentQuery[Nothing] = AttachmentQuery()

  class AttachmentQuery[T] private (attachment: Attachment[T]) {

    def title(title: String, link: Option[String]): AttachmentQuery[T] =
      new AttachmentQuery(attachment.copy(title = title.some, titleLink = link))

    def footer(footer: String, icon: Option[String]): AttachmentQuery[T] =
      new AttachmentQuery(attachment.copy(footer = footer.some, footerIcon = icon))

    def author(name: String, link: Option[String], icon: Option[String]): AttachmentQuery[T] =
      new AttachmentQuery(attachment.copy(authorName = name.some, authorLink = link, authorIcon = icon))

    def color(color: String): AttachmentQuery[T] = new AttachmentQuery(attachment.copy(color = color.some))

    def text(text: String, pretext: Option[String] = None, fallback: Option[String] = None): AttachmentQuery[T] =
      new AttachmentQuery(attachment.copy(text = text.some, pretext = pretext, fallback = fallback.orElse(text.some)))

    def image(img: String): AttachmentQuery[T] =
      new AttachmentQuery(attachment.copy(imageUrl = img.some, thumbUrl = None))

    def thumb(img: String): AttachmentQuery[T] =
      new AttachmentQuery(attachment.copy(thumbUrl = img.some, imageUrl = None))

    def fields(fields: List[AttachmentField]): AttachmentQuery[T] =
      new AttachmentQuery(attachment.copy(fields = fields))

    def action[X >: T](action: Action[X]): AttachmentQuery[X] =
      new AttachmentQuery[X](attachment.copy(actions = action :: attachment.actions).asInstanceOf[Attachment[X]])

    def make: Attachment[T] = attachment

  }

  object AttachmentQuery {
    def apply(): AttachmentQuery[Nothing] = new AttachmentQuery(Attachment[Nothing]())
  }

}

trait DialogSyntax {

  def dialog(title: String): DialogQuery[Nothing] = DialogQuery(title, ().asInstanceOf[Nothing])

  def dialog[T](title: String, state: T): DialogQuery[T] = DialogQuery(title, state)

  class DialogQuery[T] private (dialog: Dialog[T]) {

    def callbackId(id: String): DialogQuery[T] = new DialogQuery(dialog.copy(callbackId = id.some))

    def introduction(text: String): DialogQuery[T] = new DialogQuery(dialog.copy(introductionText = text.some))

    def submitLabel(label: String): DialogQuery[T] = new DialogQuery(dialog.copy(submitLabel = label.some))

    def notifyOnCancel: DialogQuery[T] = new DialogQuery(dialog.copy(notifyOnCancel = true))

    def element(element: Element): DialogQuery[T] =
      new DialogQuery[T](dialog.copy(elements = element :: dialog.elements))

    def make: Dialog[T] = dialog

  }

  object DialogQuery {
    def apply[T](title: String, state: T): DialogQuery[T] = new DialogQuery(Dialog(title = title, state = state))
  }

}

trait RouterSyntax {

  inline def handle[F[_], H, N <: Singleton](
      inline handle: H,
      name: N
  ): Handle[F, H, N, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple] =
    Handle[F, H, N, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple, EmptyTuple](
      handle
    )

  extension [L <: RouterDSL](left: L) {

    def <~>[R <: RouterDSL](right: R) = <>(left, right)

    inline def in[F[_], G[_]](
        onDialogMissing: HttpAction => AppResponse[Nothing] = _ => AppResponse.Ok(),
        onActionMissing: HttpAction => AppResponse[Nothing] = _ => AppResponse.Ok(),
        onCommandMissing: CommandAction => AppResponse[Nothing] = _ => AppResponse.Ok()
    )(using monadThrowF: MonadThrow[F], monadThrowG: MonadThrow[G]): G[Router[F]] =
      ${
        RouterMacro
          .router[F, G, L]('left, 'monadThrowF, 'monadThrowG, 'onDialogMissing, 'onActionMissing, 'onCommandMissing)
      }

  }

}

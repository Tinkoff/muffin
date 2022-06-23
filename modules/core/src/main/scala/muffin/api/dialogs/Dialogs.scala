package muffin.api.dialogs

trait Dialogs[F[_]] {
  def openDialog(triggerId: String, url: String, dialog: Dialog): F[Unit]
}

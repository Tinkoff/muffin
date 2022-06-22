package muffin.api.dialogs

import io.circe.Encoder

trait Dialogs[F[_]] {
  def openDialog(triggerId: String, url: String, dialog: Dialog): F[Unit]
}

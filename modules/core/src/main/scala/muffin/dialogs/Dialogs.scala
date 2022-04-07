package muffin.dialogs

import io.circe.Encoder

trait Dialogs[F[_]] {
  def openDialog(req: OpenDialogRequest): F[Unit]
}

package muffin.app

import io.circe.Json

trait Router[F[_]] {
  def handleAction(actionName: String, context: RawAction): F[AppResponse]

  def handleCommand(actionName: String, context: CommandContext): F[AppResponse]

  def handleDialog(actionName: String, context: DialogContext): F[AppResponse]
}

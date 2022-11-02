package muffin.router

import muffin.model.*

case class HttpResponse(data: String)

case class HttpAction(data: String)

trait Router[F[_]] {

  def handleAction(actionName: String, context: HttpAction): F[HttpResponse]

  def handleCommand(actionName: String, context: CommandAction): F[HttpResponse]

  def handleDialog(actionName: String, context: HttpAction): F[HttpResponse]

}

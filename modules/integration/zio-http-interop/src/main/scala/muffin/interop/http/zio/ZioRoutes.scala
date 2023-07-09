package muffin.interop.http.zio

import java.nio.charset.Charset

import zio.*
import zio.http.*

import muffin.model.*
import muffin.router.*

object ZioRoutes {

  def routes[R](
      router: Router[RHttp[R]]
  ): HttpApp[R, Response] =
    Http
      .collectZIO[Request]
      .apply[R, Response, Response] {
        case req @ Method.POST -> Path.root / "commands" / handler / command =>
          handleEvent[R](req)(data => router.handleCommand(s"$handler::$command", decodeFormData(data)))
        case req @ Method.POST -> Path.root / "actions" / handler / actions  =>
          handleEvent[R](req)(data => router.handleAction(s"$handler::$actions", HttpAction(data)))
        case req @ Method.POST -> Path.root / "dialogs" / handler / dialogs  =>
          handleEvent[R](req)(data => router.handleDialog(s"$handler::$dialogs", HttpAction(data)))
      }

  def handleEvent[R](
      request: Request
  )(fun: String => RIO[R, HttpResponse]): ZIO[R, Response, Response] =
    (for {
      buf      <- request.body.asString(Charset.forName("UTF-8"))
      response <- fun(buf)
    } yield Response.json(response.data)).mapError { err =>
      Response.fromHttpError(HttpError.InternalServerError(err.getMessage, Some(err)))
    }

  private def decodeFormData(str: String): CommandAction = {
    val parts =
      str.split('&').map {
        case s"$name=$value" => name -> value
      }.toMap
    CommandAction(
      ChannelId(parts("channel_id")),
      parts("channel_name"),
      parts("response_url"),
      parts("team_domain"),
      TeamId(parts("team_id")),
      parts.get("text"),
      parts("trigger_id"),
      UserId(parts("user_id")),
      parts("user_name")
    )
  }

}

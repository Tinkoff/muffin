package muffin.interop.http.zio

import java.nio.charset.Charset

import cats.effect.Sync

import zhttp.http.*
import zhttp.service.*
import zhttp.service.server.ServerChannelFactory
import zio.*

import muffin.api.*
import muffin.codec.*
import muffin.codec.CodecSupport
import muffin.model.*
import muffin.router.*

object ZRoutes {

  def routes[R, To[_], From[_]](
      router: Router[RHttp[R]],
      codec: CodecSupport[To, From]
  ): Http[HttpR[R], Throwable, Request, Response] = {
    import codec.given
    Http
      .collectZIO[Request]
      .apply[HttpR[R], Throwable, Response] {
        case req @ Method.POST -> !! / "commands" / handler / command =>
          handleEvent[HttpR[R]](req)(data => router.handleCommand(s"$handler::$command", decodeFormData(data)))
        case req @ Method.POST -> !! / "actions" / handler / actions  =>
          handleEvent[HttpR[R]](req)(data => router.handleAction(s"$handler::$actions", HttpAction(data)))
        case req @ Method.POST -> !! / "dialogs" / handler / dialogs  =>
          handleEvent[HttpR[R]](req)(data => router.handleDialog(s"$handler::$dialogs", HttpAction(data)))
      }
  }

  def handleEvent[R](
      request: Request
  )(fun: String => ZRHttp[HttpR[R], HttpResponse])(using decoder: Decode[String]): ZRHttp[HttpR[R], Response] =
    for {
      buf      <- request.body.asString(Charset.forName("UTF-8"))
      response <-
        decoder.apply(buf) match {
          case Left(error)  => ZIO.fail(error)
          case Right(value) => fun(value)
        }
    } yield Response.json(response.data)

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

package muffin

import zio.*

import java.time.{LocalDateTime, ZoneId}
import cats.effect.*
import zio.json.{JsonDecoder, JsonEncoder}
import zio.interop.catz.given
import muffin.api.*
import muffin.model.*
import muffin.dsl.*
import muffin.interop.zio.codec
import muffin.interop.zio.codec.given
import muffin.interop.http.zio.*
import muffin.router.Router
import zhttp.http.{Http, Request, Response}
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{ChannelFactory, EventLoopGroup, Server, ServerChannelFactory}

//type Api =

class SimpleCommandHandler[-R](api: ApiClient[RHttp[R], JsonEncoder, JsonDecoder]) {

  def time(command: CommandAction): ZRHttp[R, AppResponse[Nothing]] =
    api.postToChannel(command.channelId, Some(s"Current time:${LocalDateTime.now()}")).as(ok)

}

object Application extends ZIOAppDefault {

  def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    for {
      client <- ZClient[Any, Task, JsonEncoder, JsonDecoder](codec)

      cfg = ClientConfig("url", "token", "botname", "url")

      given ZoneId = ZoneId.systemDefault()

      api = new ApiClient(client, cfg)(codec)

      handler = SimpleCommandHandler(api)

      router <- handle(handler).command(_.time).in[RHttp[Any], Task]()

      routes = ZRoutes.routes(router, codec)
      _ <- Server.app(routes)
        .startDefault
        .provide(EventLoopGroup.auto(0) ++ ChannelFactory.auto)
    } yield _root_.zio.ExitCode.success

}

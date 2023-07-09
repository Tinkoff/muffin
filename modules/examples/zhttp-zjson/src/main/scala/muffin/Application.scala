package muffin

import java.time.{LocalDateTime, ZoneId}

import cats.effect.*

import zio.*
import zio.http.*
import zio.interop.catz.given
import zio.json.{JsonDecoder, JsonEncoder}

import muffin.api.*
import muffin.dsl.*
import muffin.interop.http.zio.*
import muffin.interop.json.zio.codec
import muffin.interop.json.zio.codec.given
import muffin.model.*
import muffin.router.Router

class SimpleCommandHandler[R](api: ApiClient[RHttp[R], JsonEncoder, JsonDecoder]) {

  def time(command: CommandAction): RIO[R, AppResponse] =
    api.postToChannel(command.channelId, Some(s"Current time:${LocalDateTime.now()}")).as(ok)

}

object Application extends ZIOAppDefault {

  def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    (for {
      client <- ZioClient[Any, Task, JsonEncoder, JsonDecoder](codec)

      cfg = ClientConfig("url", "token", "botname", "url")

      given ZoneId = ZoneId.systemDefault()

      api = new ApiClient(client, cfg)(codec)

      handler = SimpleCommandHandler(api)

      router <- handle(handler, "kek").command(_.time).in[RHttp[Client], Task]()

      routes = ZioRoutes.routes(router)
      _ <- Server.serve(routes)
    } yield _root_.zio.ExitCode.success)
      .provide(Server.default, Client.default)

}

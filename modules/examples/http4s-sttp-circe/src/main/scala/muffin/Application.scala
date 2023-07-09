package muffin

import java.time.{LocalDateTime, ZoneId}

import cats.effect.*
import cats.effect.IO.given

import com.comcast.ip4s.*
import io.circe.{Decoder, Encoder}
import org.http4s.ember.server.*
import org.http4s.server.Router
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import muffin.api.*
import muffin.dsl.*
import muffin.interop.http.http4s.Http4sRoute
import muffin.interop.http.sttp.SttpClient
import muffin.interop.json.circe.codec
import muffin.interop.json.circe.codec.given
import muffin.model.*

type Api = ApiClient[IO, Encoder, Decoder]

class SimpleCommandHandler(api: Api) {

  def time(command: CommandAction): IO[AppResponse] =
    api.postToChannel(command.channelId, Some(s"Current time:${LocalDateTime.now()}")).as(ok)

}

object Application extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    for {
      backend <- AsyncHttpClientCatsBackend[IO]()
      client  <- SttpClient(backend, codec)

      given ZoneId = ZoneId.systemDefault()
      cfg          = ClientConfig("url", "token", "botname", "url")
      api          = new ApiClient(client, cfg)(codec)

      handler    = SimpleCommandHandler(api)
      timeHandle = handle(handler, "kek").command(_.time)

      router <- timeHandle.in[IO, IO]()

      routes = Http4sRoute.routes(router)

      _ <- EmberServerBuilder.default[IO].withHost(ipv4"0.0.0.0").withPort(port"8080")
        .withHttpApp(Router("/" -> routes).orNotFound).build.use(_ => IO.never)
    } yield ExitCode.Success

}

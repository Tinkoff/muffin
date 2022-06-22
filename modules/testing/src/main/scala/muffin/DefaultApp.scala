package muffin

import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.Task
import zio.interop.catz.given
import muffin.interop.circe.codec
import muffin.interop.circe.codec.given
import io.circe.*
import muffin.api.{ApiClient, ClientConfig}
import muffin.codec.Encode
import muffin.http.HttpClient
import muffin.interop.http.SttpClient

import java.time.ZoneId

type CirceApi[F[_]] = ApiClient[F, Json, Encoder, Decoder]

object DefaultApp {
  def apply(cfg: ClientConfig): Task[CirceApi[Task]] =
    for {
      backend <- HttpClientZioBackend()
      client <- SttpClient[Task, Task, Encoder, Decoder](backend)
      given ZoneId <- Task.succeed(ZoneId.systemDefault())
    } yield new ApiClient[Task, Json, Encoder, Decoder](client, cfg)(codec)
}

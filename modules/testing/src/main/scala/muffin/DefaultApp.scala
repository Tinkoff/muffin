package muffin

import muffin.ApiClient
import muffin.http.SttpClient
import muffin.http.HttpClient
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.Task
import zio.interop.catz.given
import muffin.interop.circe.*
import io.circe.*

import java.time.ZoneId

type CirceApi[F[_]] = ApiClient[F, Encoder, Decoder]

object DefaultApp {
  def apply(cfg: ClientConfig): Task[CirceApi[Task]] =
    for {
      backend <- HttpClientZioBackend()
      client <- SttpClient[Task, Task, Encoder, Decoder](backend)(codec.DecoderFrom, codec.EncoderTo)
      given ZoneId <- Task.succeed(ZoneId.systemDefault())
    } yield new ApiClient[Task, Encoder, Decoder](client, cfg)(codec)
}

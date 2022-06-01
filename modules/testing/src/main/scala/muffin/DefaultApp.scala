package muffin

import muffin.ApiClient
import muffin.http.SttpClient
import muffin.{ClientConfig, HttpClient}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.Task
import zio.interop.catz.given

object DefaultApp {
  def apply(cfg: ClientConfig): Task[ApiClient[Task]] =
    for {
      backend <- HttpClientZioBackend()
      given SttpClient[Task] <- SttpClient[Task, Task](backend)
    } yield ApiClient[Task](cfg)
}

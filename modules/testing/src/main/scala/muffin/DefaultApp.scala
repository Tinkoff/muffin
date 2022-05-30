package muffin

import muffin.app.{Mattermost, AppContext}
import muffin.ApiClient
import muffin.http.SttpClient
import muffin.{ClientConfig, HttpClient}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.Task
import zio.interop.catz.given

object DefaultApp {
  def apply(cfg: ClientConfig): Task[Mattermost[Task]] =
    for {
      backend <- HttpClientZioBackend()

      http <- SttpClient[Task, Task](backend)
      client = {
        given h: HttpClient[Task] = http
        ApiClient[Task](cfg)
      }

      ctx = AppContext(client)

    } yield new Mattermost(ctx, "")
}

package muffin

import io.circe.{Json, JsonObject}
import muffin.app.{AppResponse, Mattermost}
import muffin.http.SttpClient
import muffin.emoji.*
import muffin.posts.*
import muffin.predef.*
import muffin.reactions.*
import muffin.DefaultApp
import muffin.dialogs.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.{Task, ZIOAppDefault}
import zio.interop.catz.given

import java.io.File
import zio.interop.catz.implicits.given

object Application extends ZIOAppDefault {

  val token = "ayxxty8s1jy6mcsnsp9octpqqe"

  val botId = UserId("jbrahfps57f458qsdth6urbsnw")

  val run =
    for {
      app: Mattermost[Task] <- DefaultApp(
        ClientConfig("http://localhost:8065/api/v4", token, botId)
      )

      _ = app.command("kek") { (ctx, action) =>
        ctx.client
          .openDialog(
            OpenDialogRequest(
              action.triggerId,
              "http://host.docker.internal:8080/dialogs/superdialog",
              Dialog(
                "id",
                "title",
                "intoduction",
                List(Text("display", "name")),
                Some("submit submit"),
                true,
                "State"
              )
            )
          )
          .as(ctx.ok)
      }

      _ = app.dialog("coffee") { (ctx, action) =>
        ctx.client
          .createPost(CreatePostRequest(action.channel_id, action.submission.toString()))
          .as(ctx.ok)
      }

      _ <- DefaultServer(app).useForever
    } yield ()

}

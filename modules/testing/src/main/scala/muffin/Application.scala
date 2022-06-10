package muffin

import cats.MonadThrow
import io.circe.{Json, JsonObject}
import io.circe.{Json, JsonObject, Codec}
import muffin.app.{DialogSubmissionValue, *}
import muffin.http.SttpClient
import muffin.emoji.*
import muffin.posts.*
import muffin.predef.*
import muffin.reactions.*
import muffin.DefaultApp
import muffin.dialogs.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.{Task, ZIOAppDefault}
import cats.syntax.all.given
import zio.interop.catz.given

import java.io.File
import zio.interop.catz.implicits.given

case class A(str: String) derives Codec.AsObject

class HandlerA[F[_]: MonadThrow](client: CirceApi[F]) {
  def kekA(action: CommandContext): F[AppResponse] =
    client
      .openDialog(
        OpenDialogRequest(
          action.triggerId,
          client.dialog("superdialog"),
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
      .map(_ => AppResponse.Ok())

  def acc(action: RawAction):F[AppResponse] = ???

  def actionA(dialog: Action[A]): F[AppResponse] =
    client
      .openDialog(
        OpenDialogRequest(
          dialog.triggerId,
          client.dialog("superdialog"),
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
      .map(_ => AppResponse.Ok())
}

class HandlerB[F[_] : MonadThrow](client: CirceApi[F]) {
  def kekB(action: CommandContext): F[AppResponse] =
    client
      .openDialog(
        OpenDialogRequest(
          action.triggerId,
          client.dialog("superdialog"),
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
      .map(_ => AppResponse.Ok())

  def actionB(dialog: Action[A]): F[AppResponse] =
    client
      .openDialog(
        OpenDialogRequest(
          dialog.triggerId,
          client.dialog("superdialog"),
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
      .map(_ => AppResponse.Ok())
}

object Application extends ZIOAppDefault {

  val token = "ayxxty8s1jy6mcsnsp9octpqqe"

  val run =
    for {
      app: CirceApi[Task] <- DefaultApp(
        ClientConfig(
          "http://localhost:8065/api/v4",
          token,
          "name",
          "http://host.docker.internal:8080"
        )
      )

      router <- {
        given HandlerA[Task] = new HandlerA[Task](app)
        given HandlerB[Task] = new HandlerB[Task](app)

        RouterBuilder[Task]
          .command[HandlerA[Task], "kekA"]
          .rawAction[HandlerA[Task], "acc"]
          .action[HandlerA[Task], A, "actionA"]
          .action[HandlerB[Task], A, "actionB"]
          .unexpected((name, action) => Task.succeed(AppResponse.Ok()))
          .build[Task]
      }

      _ <- DefaultServer(router).useForever
    } yield ()

}

package muffin

import cats.effect.Async
import com.comcast.ip4s.Literals.ipv4
import sttp.tapir.*
import sttp.tapir.given
import sttp.tapir.json.circe.*
import io.circe.Json
import muffin.app.*
import org.http4s.server.Router
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import org.http4s.ember.server.*
import zio.{Clock, Runtime, Task}
import zio.interop.catz.given
import org.http4s.implicits.given
import com.comcast.ip4s.*
import muffin.predef.*
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.RequestResult
import sttp.tapir.server.interceptor.log.{DefaultServerLog, ServerLog}
import sttp.tapir.server.interceptor.reject.RejectHandler
import sttp.tapir.server.model.ValuedEndpointOutput
import cats.syntax.all.given
import muffin.posts.*

object DefaultServer {

  given Schema[ResponseType] = Schema.derived

  given Schema[AppResponse] = Schema.derived
  given Schema[Attachment] = Schema.string[Attachment]

  given Schema[RawAction] = Schema.derived


  given Schema[DialogContext] = Schema.derived

  given Schema[DialogSubmissionValue] = Schema.derived

  given Schema[UserId] = Schema.schemaForString.map(UserId(_).some)(_.toString)
  given Schema[ChannelId] = Schema.schemaForString.map(ChannelId(_).some)(_.toString)
  given Schema[Login] = Schema.schemaForString.map(Login(_).some)(_.toString)
  given Schema[MessageId] = Schema.schemaForString.map(MessageId(_).some)(_.toString)


  def commands[F[_]: Async](app: Router[F]) = endpoint.post
    .in("commands")
    .in(paths)
    .in(formBody[Map[String, String]])
    .out(jsonBody[AppResponse])
    .serverLogicSuccess { case (segments, params) =>
      app.handleCommand(
        segments.last,
        CommandContext(
          channelId = ChannelId(params("channel_id")),
          channelName = params("channel_name"),
          responseUrl = params("response_url"),
          teamDomain = params("team_domain"),
          teamId = params("team_id"),
          text = params.get("text"),
          triggerId = params("trigger_id"),
          userId = UserId(params("user_id")),
          userName = params("user_name")
        )
      )
    }

  def actions[F[_]: Async](app: Router[F]) = endpoint.post
    .in("commands")
    .in(paths)
    .in(jsonBody[RawAction])
    .out(jsonBody[AppResponse])
    .serverLogicSuccess { case (segments, param) =>
      app.handleAction(segments.last, param)
    }

  def dialogs[F[_]: Async](app: Router[F]) = endpoint.post
    .in("dialogs")
    .in(paths)
    .in(jsonBody[DialogContext])
    .out(jsonBody[AppResponse])
    .serverLogicSuccess { case (segments, param) =>
      app.handleDialog(segments.last, param)
    }

  private def server[F[_]: Async](app: Router[F]) =
    Router(
      "/" -> Http4sServerInterpreter[F]()
        .toRoutes(List(commands(app), actions(app), dialogs(app)))
    ).orNotFound

  def apply(app: Router[Task])(using Runtime[Clock]) =
    EmberServerBuilder
      .default[Task]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"8080")
      .withHttpApp(server(app))
      .build

}

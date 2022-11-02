package muffin.interop.http.zio

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.all.given
import fs2.*

import org.http4s.*
import org.http4s.FormDataDecoder.given
import org.http4s.dsl.io.*
import org.typelevel.ci.CIString

import muffin.api.*
import muffin.codec.{CodecSupport, Decode, Encode}
import muffin.model.*
import muffin.router.*

object Http4sRoute {

  def routes[F[_], To[_], From[_]](router: Router[F], codec: CodecSupport[To, From])(using
      C: Concurrent[F]
  ): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "commands" / handler / command =>
        import codec.given
        handleEvent[F, UrlForm](req) { form =>
          for {
            context <-
              (
                form.get("channel_id").get(0),
                form.get("channel_name").get(0),
                form.get("response_url").get(0),
                form.get("team_domain").get(0),
                form.get("team_id").get(0),
                form.get("trigger_id").get(0),
                form.get("user_id").get(0),
                form.get("user_name").get(0)
              ).mapN[CommandAction] {
                (channelId, channelName, responseUrl, teamDomain, teamId, triggerId, userId, userName) =>
                  CommandAction(
                    ChannelId(channelId),
                    channelName,
                    responseUrl,
                    teamDomain,
                    TeamId(teamId),
                    form.get("text").get(0),
                    triggerId,
                    UserId(userId),
                    userName
                  )
              } match {
                case Some(action) => C.pure(action)
                case None         => C.raiseError[CommandAction](new Exception(s"Cannot parse command ${form.toString}"))
              }

            res <- router.handleCommand(s"$handler::$command", context)
          } yield res
        }
      case req @ POST -> Root / "actions" / handler / action   =>
        import codec.given
        handleEvent[F, HttpAction](req)(router.handleAction(s"$handler::$action", _))
      case req @ POST -> Root / "dialogs" / handler / dialog   =>
        import codec.given
        handleEvent[F, HttpAction](req)(router.handleDialog(s"$handler::$dialog", _))
    }

  private def handleEvent[F[_]: Concurrent, In](
      request: Request[F]
  )(fun: In => F[HttpResponse])(using decoder: EntityDecoder[F, In]): F[Response[F]] =
    for {
      req: In <- request.as[In]
      res     <- fun(req)
    } yield Response(
      headers = Headers(Header.Raw(CIString("Content-Type"), "application/json")),
      entity = Entity.strict(scodec.bits.ByteVector(res.data.getBytes))
    )

  private given [F[_]: Concurrent, T](using decoder: Decode[T]): EntityDecoder[F, T] =
    new EntityDecoder[F, T] {

      override def decode(m: Media[F], strict: Boolean): DecodeResult[F, T] =
        EitherT(m.bodyText.compile.string.map(str => decoder(str)))
          .leftMap(error => MalformedMessageBodyFailure("Can't parse body", error.some))

      override def consumes: Set[MediaRange] = Set(MediaType.application.json)

    }

  private given [F[_]: Concurrent]: EntityDecoder[F, HttpAction] =
    new EntityDecoder[F, HttpAction] {

      override def decode(m: Media[F], strict: Boolean): DecodeResult[F, HttpAction] =
        DecodeResult.success(
          m.bodyText.compile.string.map(HttpAction(_))
        )

      override def consumes: Set[MediaRange] = Set(MediaType.application.json)

    }

}

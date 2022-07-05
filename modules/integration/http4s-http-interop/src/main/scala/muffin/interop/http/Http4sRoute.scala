package muffin.interop.http

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.all.given
import muffin.codec.{CodecSupport, Decode, Encode}
import muffin.input.{
  AppResponse,
  CommandContext,
  DialogContext,
  RawAction,
  Router
}
import org.http4s.*
import org.http4s.dsl.io.*
import fs2.*
import muffin.predef.*
import org.http4s.FormDataDecoder.given
import org.typelevel.ci.CIString

object Http4sRoute {
  def routes[F[_], R, To[_], From[_]](
    router: Router[F, R],
    codec: CodecSupport[R, To, From]
  )(using C: Concurrent[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "commands" / command =>
      import codec.given
      handleEvent[F, UrlForm](req) { form =>
        for {
          context <- (
            form.get("channel_id").get(0),
            form.get("channel_name").get(0),
            form.get("response_url").get(0),
            form.get("team_domain").get(0),
            form.get("team_id").get(0),
            form.get("trigger_id").get(0),
            form.get("user_id").get(0),
            form.get("user_name").get(0)
          ).mapN[CommandContext] {
            (
              channelId,
              channelName,
              responseUrl,
              teamDomain,
              teamId,
              triggerId,
              userId,
              userName
            ) =>
              CommandContext(
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
            case Some(context) => C.pure(context)
            case None =>
              C.raiseError[CommandContext](
                new Exception(s"Cannot parse command ${form.toString}")
              )
          }

          res <- router.handleCommand(command, context)
        } yield res
      }
    case req @ POST -> Root / "actions" / action =>
      import codec.given
      handleEvent[F, RawAction[R]](req)(router.handleAction(action, _))
    case req @ POST -> Root / "dialogs" / dialog =>
      import codec.given
      handleEvent[F, DialogContext](req)(router.handleDialog(dialog, _))
  }

  private def handleEvent[F[_]: Concurrent, In](
    request: Request[F]
  )(fun: In => F[AppResponse])(using
    decoder: EntityDecoder[F, In],
    encoder: Encode[AppResponse]
  ): F[Response[F]] =
    for {
      req: In <- request.as[In]
      res <- fun(req)
    } yield Response(
      headers =
        Headers(Header.Raw(CIString("Content-Type"), "application/json")),
      body = EntityEncoder
        .simple[F, AppResponse]()(resp =>
          Chunk.byteVector(scodec.bits.ByteVector(encoder.apply(resp).getBytes))
        )
        .toEntity(res)
        .body
    )

  private given [F[_]: Concurrent, T](using
    decoder: Decode[T]
  ): EntityDecoder[F, T] = new EntityDecoder[F, T] {
    override def decode(m: Media[F], strict: Boolean): DecodeResult[F, T] =
      EitherT(
        m.bodyText.compile.string
          .map { str =>
            println(s"QWEQWEQWEQWEQWEQWEQWE: $str")
            decoder(str)
          }
      )
        .leftMap(error =>
          MalformedMessageBodyFailure("Can't parse body", error.some)
        )

    override def consumes: Set[MediaRange] = Set(MediaType.application.json)
  }

}

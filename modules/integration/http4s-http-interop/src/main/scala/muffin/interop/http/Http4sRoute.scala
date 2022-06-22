package muffin.interop.http

import cats.MonadThrow
import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.all.given
import muffin.codec.{CodecSupport, Decode, Encode}
import muffin.input.{AppResponse, CommandContext, RawAction, Router, DialogContext}
import org.http4s.*
import org.http4s.dsl.io.*
import fs2.*

class Http4sRoute {
  def routes[F[_] : Sync, R, To[_], From[_]](router: Router[F, R], codec: CodecSupport[R, To, From]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req@POST -> Root / "commands" / command =>
      import codec.given
      handleEvent[F, CommandContext](req)(router.handleCommand(command, _))
    case req@POST -> Root / "actions" / action =>
      import codec.given
      handleEvent[F, RawAction[R]](req)(router.handleAction(action, _))
    case req@POST -> Root / "dialogs" / dialog =>
      import codec.given
      handleEvent[F, DialogContext](req)(router.handleDialog(dialog, _))
  }

  private def handleEvent[F[_] : Sync, In](request: Request[F])(fun: In => F[AppResponse])(using decoder: Decode[In], encoder: Encode[AppResponse]): F[Response[F]] =
    for {
      req: In <- request.as[In]
      res <- fun(req)
    } yield Response(body = EntityEncoder.simple[F, AppResponse]()(resp =>
      Chunk.byteVector(scodec.bits.ByteVector(encoder.apply(resp).getBytes))
    ).toEntity(res).body)

  private given[F[_] : Sync, T](using decoder: Decode[T]): EntityDecoder[F, T] = new EntityDecoder[F, T] {
    override def decode(m: Media[F], strict: Boolean): DecodeResult[F, T] = {
      EitherT(m.bodyText.compile.string.map(decoder(_))).leftMap(
        error =>
          MalformedMessageBodyFailure("Can't parse body", error.some)
      )
    }

    override def consumes: Set[MediaRange] = Set(MediaType.application.json)
  }

}

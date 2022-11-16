package muffin.interop.http.zio

import java.io.File

import cats.{~>, Applicative, MonadThrow}
import cats.effect.Sync
import cats.syntax.all.given

import sttp.client3.*
import sttp.client3.given
import sttp.model.{Header, Method as SMethod, Uri}

import muffin.codec.*
import muffin.http.*

class SttpClient[F[_]: MonadThrow, To[_], From[_]](backend: SttpBackend[F, Any], codec: CodecSupport[To, From])
  extends HttpClient[F, To, From] {

  import codec.given

  def request[In: To, Out: From](
      url: String,
      method: Method,
      body: Body[In],
      headers: Map[String, String],
      params: Params => Params
  ): F[Out] = {
    val req = basicRequest
      .method(
        method match {
          case Method.Get    => SMethod.GET
          case Method.Post   => SMethod.POST
          case Method.Put    => SMethod.PUT
          case Method.Delete => SMethod.DELETE
          case Method.Patch  => SMethod.PATCH
        },
        Uri.unsafeParse(url + params(Params.Empty).mkString)
      )
      .headers(headers)
      .mapResponse(_.flatMap(response => summon[Decode[Out]].apply(response).left.map(_.getMessage)))

    (
      body match {
        case body: Body.RawJson    =>
          backend.send(req.body(body.value, "UTF-8").header("Content-Type", "application/json"))
        case body: Body.Json[?]    =>
          backend
            .send(req.body(summon[Encode[In]].apply(body.value), "UTF-8").header("Content-Type", "application/json"))
        case Body.Multipart(parts) =>
          backend.send(
            req
              .multipartBody(
                parts.map {
                  case MultipartElement.StringElement(name, value) => multipart(name, value)
                  case MultipartElement.FileElement(name, value)   => multipartFile(name, value)
                }
              )
              .header("Content-Type", "multipart/form-data")
          )
        case Body.Empty            => backend.send(req)
      }
    ).map(_.body)
      .flatMap {
        case Left(value)  => MonadThrow[F].raiseError(new Exception(value))
        case Right(value) => value.pure[F]
      }
  }

}

object SttpClient {

  def apply[I[_]: Sync, F[_]: MonadThrow, To[_], From[_]](
      backend: SttpBackend[F, Any],
      codec: CodecSupport[To, From]
  ): I[SttpClient[F, To, From]] = Sync[I].delay(new SttpClient[F, To, From](backend, codec))

}

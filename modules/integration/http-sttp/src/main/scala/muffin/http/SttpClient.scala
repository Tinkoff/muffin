package muffin.http

import cats.{Applicative, MonadThrow, ~>}
import io.circe.Decoder
import io.circe.parser.parse
import muffin.http.{Body, HttpClient, Method, MultipartElement}
import sttp.client3.*
import sttp.client3.given
import sttp.model.{Header, Method as SMethod, Uri}
import io.circe.syntax.given
import cats.syntax.all.given
import muffin.codec.{Decode, Encode}
import java.io.File


class SttpClient[F[_] : MonadThrow, To[_], From[_]](backend: SttpBackend[F, Any])(fk: From ~> Decode, tk: To ~> Encode)
  extends HttpClient[F, To, From] {
  def request[In: To, Out: From](
                              url: String,
                              method: Method,
                              body: Body[In],
                              headers: Map[String, String]
                            ): F[Out] = {
    val req = basicRequest
      .method(
        method match {
          case Method.Get => SMethod.GET
          case Method.Post => SMethod.POST
          case Method.Put => SMethod.PUT
          case Method.Delete => SMethod.DELETE
        },
        Uri.unsafeParse(url)
      )
      .mapResponse(_.flatMap(fk(summon[From[Out]]).apply(_).left.map(_.getMessage)))
      .headers(headers)

    (body match {
      case b: Body.Json[In] =>
        backend.send(
          req
            .body(tk.apply(summon[To[In]]).apply(b.value), "UTF-8")
            .header("Content-Type", "application/json")
        )
      case Body.Multipart(parts) =>
        backend.send(
          req
            .multipartBody(parts.map {
              case MultipartElement.StringElement(name, value) =>
                multipart(name, value)
              case MultipartElement.FileElement(name, value) =>
                multipartFile(name, value)
            })
            .header("Content-Type", "multipart/form-data")
        )
      case Body.Empty => backend.send(req)
    })
      .map(_.body).flatMap {
      case Left(value) => MonadThrow[F].raiseError(new Exception(value))
      case Right(value) => value.pure[F]
    }
  }
}

object SttpClient {
  def apply[I[_] : Applicative, F[_] : MonadThrow, To[_], From[_]](backend: SttpBackend[F, Any])(fk: From ~> Decode, tk: To ~> Encode): I[SttpClient[F, To, From]] =
    new SttpClient[F, To, From](backend)(fk, tk).pure[I]
}

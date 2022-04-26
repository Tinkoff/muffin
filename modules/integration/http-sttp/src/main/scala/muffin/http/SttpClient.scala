package muffin.http

import cats.{Applicative, MonadThrow}
import io.circe.Decoder
import io.circe.parser.parse
import muffin.{Body, HttpClient, Method, MultipartElement}
import sttp.client3.*
import sttp.client3.given
import sttp.model.{Header, Method as SMethod, Uri}
import io.circe.syntax.given
import cats.syntax.all.given

import java.io.File

class SttpClient[F[_]: MonadThrow](backend: SttpBackend[F, Any])
    extends HttpClient[F] {
  def request[In, Out: Decoder](
    url: String,
    method: Method,
    body: Body[In],
    headers: Map[String, String]
  ): F[Out] = {
    val req = basicRequest
      .method(
        method match {
          case Method.Get    => SMethod.GET
          case Method.Post   => SMethod.POST
          case Method.Delete => SMethod.DELETE
        },
        Uri.unsafeParse(url)
      )
      .mapResponse(
        _.map(a=>{
          println(a)
          a
        }).flatMap(
          parse(_)
            .flatMap(_.as[Out])
            .left
            .map(_.getMessage)
        )
          .left.map(
            a=> {
              println(a)
              a
            }
          )
      )
      .headers(headers)

    (body match {
      case b: Body.Json[In] =>
        println(b.as)
        backend.send(
          req
            .body(b.as, "UTF-8")
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
      case Left(value)  => MonadThrow[F].raiseError(new Exception(value))
      case Right(value) => value.pure[F]
    }
  }
}

object SttpClient {
  def apply[I[_]: Applicative, F[_]: MonadThrow](
    backend: SttpBackend[F, Any]
  ): I[SttpClient[F]] =
    new SttpClient[F](backend).pure[I]
}

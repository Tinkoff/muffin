package muffin.http

import cats.{Applicative, MonadThrow}
import io.circe.Decoder
import io.circe.parser.parse
import muffin.Method.{Delete, Get, Post}
import muffin.{Body, HttpClient, Method}
import sttp.client3.{NoBody, Request, RequestBody, StringBody, SttpBackend, basicRequest}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.{Header, Uri, Method as SMethod}
import io.circe.syntax.given
import cats.syntax.all.given


class SttpClient[F[_]: MonadThrow](backend: SttpBackend[F, Any]) extends HttpClient[F] {
  def request[In, Out: Decoder](
      url: String,
      method: Method,
      body: Body[In],
      headers: Map[String, String]
  ): F[Out] = {
   val req = basicRequest.method(
      method match {
        case Get => SMethod.GET
        case Post => SMethod.POST
        case Delete => SMethod.DELETE
      },
      Uri.unsafeParse(url)
    )
     .headers(headers + ("Content-Type"-> "application/json"))
     .mapResponse(_.flatMap(parse(_)
       .flatMap(_.as[Out])
       .left
       .map(_.getMessage)
     ))

   (body match {
     case b:Body.Json[In] => backend.send(req.body(b.as, "UTF-8"))
     case Body.Empty => backend.send(req)
     }).map(_.body).flatMap {
      case Left(value) => MonadThrow[F].raiseError(new Exception(value))
      case Right(value) => value.pure[F]
   }
  }
}

object SttpClient {
  def apply[I[_]: Applicative, F[_]:MonadThrow](backend: SttpBackend[F, Any]): I[SttpClient[F]] = {
    new SttpClient[F](backend).pure[I]
  }
}

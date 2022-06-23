package muffin.interop.http

import cats.~>
import cats.effect.Sync
import muffin.codec.Encode
import muffin.http.Method.{Delete, Get, Patch, Post, Put}
import zhttp.http.{Headers, HttpData, Method as ZMethod}
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import muffin.codec.*
import muffin.http.{Body, HttpClient, Method}
import zio.*

class ZioClient[To[_], From[_]](using tk: To ~> Encode, fk: From ~> Decode) extends HttpClient[ZioClient.ZHttp, To, From] {
  def request[In: To, Out: From](url: String, method: Method, body: Body[In], headers: Map[String, String]): ZioClient.ZHttp[Out] =
    Client.request(
      url,
      method match {
        case Get => ZMethod.GET
        case Post => ZMethod.POST
        case Delete => ZMethod.DELETE
        case Put => ZMethod.PUT
        case Patch => ZMethod.PATCH
      },
      Headers(headers.toList),
      content = body match {
        case Body.Empty => HttpData.empty
        case Body.Json(value) =>
          HttpData.fromString(tk.apply(summon[To[In]]).apply(value))
        case Body.Multipart(_) => throw new Exception("ZIO Backend don't support multipart")
      }
    ).flatMap(_.bodyAsString).flatMap {
      fk(summon[From[Out]]).apply(_) match
        case Left(value) => ZIO.fail(value)
        case Right(value) => ZIO.succeed(value)
    }
}

object ZioClient {
  type ZHttp[A] = ZIO[EventLoopGroup & ChannelFactory, Throwable, A]

  def apply[I[_]: Sync, To[_], From[_]](using tk: To ~> Encode, fk: From ~> Decode): I[HttpClient[ZHttp, To, From]] =
    Sync[I].delay(new ZioClient[To, From])
}

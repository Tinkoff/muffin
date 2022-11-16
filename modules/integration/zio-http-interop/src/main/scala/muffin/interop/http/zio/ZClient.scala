package muffin.interop.http.zio

import java.nio.charset.Charset

import cats.effect.Sync

import zhttp.http.{Body as ZBody, Headers, Method as ZMethod}
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.*

import muffin.codec.*
import muffin.http.*

class ZClient[R, To[_], From[_]](codec: CodecSupport[To, From]) extends HttpClient[RHttp[R], To, From] {

  import codec.given

  def request[In: To, Out: From](
      url: String,
      method: Method,
      body: Body[In],
      headers: Map[String, String],
      params: Params => Params
  ): ZRHttp[R, Out] =
    Client
      .request(
        url + params(Params.Empty).mkString,
        method match {
          case Method.Get    => ZMethod.GET
          case Method.Post   => ZMethod.POST
          case Method.Delete => ZMethod.DELETE
          case Method.Put    => ZMethod.PUT
          case Method.Patch  => ZMethod.PATCH
        },
        Headers(headers.toList),
        content =
          body match {
            case Body.Empty          => ZBody.empty
            case Body.Json(value)    => ZBody.fromString(summon[Encode[In]].apply(value))
            case Body.RawJson(value) => ZBody.fromString(value)
            case Body.Multipart(_)   => throw new Exception("ZIO Backend don't support multipart")
          }
      )
      .flatMap(_.body.asString(Charset.defaultCharset()))
      .flatMap {
        summon[Decode[Out]].apply(_) match {
          case Left(value)  => ZIO.fail(value)
          case Right(value) => ZIO.succeed(value)
        }
      }

}

object ZClient {

  def apply[R, I[_]: Sync, To[_], From[_]](codec: CodecSupport[To, From]): I[ZClient[R, To, From]] =
    Sync[I].delay(
      new ZClient[R, To, From](codec)
    )

}

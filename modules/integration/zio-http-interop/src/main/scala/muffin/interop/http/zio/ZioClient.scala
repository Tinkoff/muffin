package muffin.interop.http.zio

import java.nio.charset.Charset

import cats.effect.Sync

import zio.*
import zio.http.{Body as ZBody, Method as ZMethod, *}

import muffin.codec.*
import muffin.http.*

class ZioClient[R, To[_], From[_]](codec: CodecSupport[To, From]) extends HttpClient[RHttp[R with Client], To, From] {

  import codec.given

  def request[In: To, Out: From](
      url: String,
      method: Method,
      body: Body[In],
      headers: Map[String, String],
      params: Params => Params
  ): RIO[R with Client, Out] =
    for {
      requestBody <-
        body match {
          case Body.Empty            => ZIO.attempt(ZBody.empty)
          case Body.Json(value)      => ZIO.attempt(ZBody.fromString(Encode[In].apply(value)))
          case Body.RawJson(value)   => ZIO.attempt(ZBody.fromString(value))
          case Body.Multipart(parts) =>
            for {
              boundary <- Boundary.randomUUID
              form = Form.apply(Chunk.fromIterable(
                parts.map {
                  case MultipartElement.StringElement(name, value) => FormField.textField(name, value)
                  case MultipartElement.FileElement(name, value)   =>
                    FormField.binaryField(
                      name,
                      Chunk.fromArray(value),
                      MediaType.apply("application", "octet-stream", false, true)
                    )
                }
              ))
            } yield ZBody.fromMultipartForm(form, boundary)
        }

      response <- Client
        .request(
          url + params(Params.Empty).mkString,
          method match {
            case Method.Get    => ZMethod.GET
            case Method.Post   => ZMethod.POST
            case Method.Delete => ZMethod.DELETE
            case Method.Put    => ZMethod.PUT
            case Method.Patch  => ZMethod.PATCH
          },
          Headers(headers.map(Header.Custom.apply).toList),
          content = requestBody
        )

      stringResponse <- response.body.asString(Charset.defaultCharset())
      res            <-
        Decode[Out].apply(stringResponse) match {
          case Left(value)  => ZIO.fail(value)
          case Right(value) => ZIO.succeed(value)
        }
    } yield res

}

object ZioClient {

  def apply[R, I[_]: Sync, To[_], From[_]](codec: CodecSupport[To, From]): I[ZioClient[R, To, From]] =
    Sync[I].delay(
      new ZioClient[R, To, From](codec)
    )

}

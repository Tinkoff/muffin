package muffin.interop.json.zio

import cats.effect.IO
import cats.syntax.all.*

import org.scalatest.*
import org.scalatest.featurespec.AsyncFeatureSpec
import zio.json.*

import muffin.api.ApiTest
import muffin.http.{Body, HttpClient, Method, Params}

class ZioApiTest extends ApiTest[JsonEncoder, JsonDecoder]("zio", codec) {

  protected def toContext: JsonEncoder[AppContext]   = JsonEncoder.derived[AppContext]
  protected def fromContext: JsonDecoder[AppContext] = JsonDecoder.derived[AppContext]

  protected def httpClient: HttpClient[IO, JsonEncoder, JsonDecoder] =
    new HttpClient[IO, JsonEncoder, JsonDecoder] {

      def request[In: JsonEncoder, Out: JsonDecoder](
          url: String,
          method: Method,
          body: Body[In],
          headers: Map[String, String],
          params: Params => Params
      ): IO[Out] =
        (body match {
          case Body.Empty            => testRequest(url, method, None, params(Params.Empty))
          case Body.Json(value)      =>
            testRequest(url, method, JsonEncoder[In].encodeJson(value, None).toString.some, params(Params.Empty))
          case Body.RawJson(value)   => testRequest(url, method, value.some, params(Params.Empty))
          case Body.Multipart(parts) => ??? // TODO implement file tests
        }).flatMap(str =>
          str.fromJson[Out] match {
            case Left(message) => IO.raiseError(new Exception(message))
            case Right(value)  => value.pure[IO]
          }
        )

    }

}

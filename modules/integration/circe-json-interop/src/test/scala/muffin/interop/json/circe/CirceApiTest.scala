package muffin.interop.json.circe

import cats.effect.IO
import cats.syntax.all.*

import io.circe.*
import org.scalatest.*
import org.scalatest.featurespec.AsyncFeatureSpec

import muffin.api.ApiTest
import muffin.http.{Body, HttpClient, Method, Params}

class CirceApiTest extends ApiTest[Encoder, Decoder]("circe", codec) {

  protected def toContext: Encoder[AppContext]   = io.circe.Derivation.summonEncoder[AppContext]
  protected def fromContext: Decoder[AppContext] = io.circe.Derivation.summonDecoder[AppContext]

  protected def httpClient: HttpClient[IO, Encoder, Decoder] =
    new HttpClient[IO, Encoder, Decoder] {

      def request[In: Encoder, Out: Decoder](
          url: String,
          method: Method,
          body: Body[In],
          headers: Map[String, String],
          params: Params => Params
      ): IO[Out] =
        (body match {
          case Body.Empty            => testRequest(url, method, None, params(Params.Empty))
          case Body.Json(value)      =>
            testRequest(url, method, Encoder[In].apply(value).noSpaces.some, params(Params.Empty))
          case Body.RawJson(value)   =>
            testRequest(url, method, parser.parse(value).map(_.noSpaces).toOption, params(Params.Empty))
          case Body.Multipart(parts) => ???
        }).flatMap(parseJson(_))

    }

}

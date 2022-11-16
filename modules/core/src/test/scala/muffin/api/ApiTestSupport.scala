package muffin.api

import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.concurrent.Future

import cats.effect.{IO, Resource}
import cats.syntax.all.{*, given}

import io.circe.{parser, Decoder, HCursor, Json}
import org.scalactic.source.Position
import org.scalatest.*
import org.scalatest.featurespec.AsyncFeatureSpec

import muffin.http.{Method, Params}

trait ApiTestSupport extends AsyncFeatureSpec {

  protected def loadResource(path: String): IO[String] =
    Resource.fromResourcesAs(path) { in =>
      IO(new String(in.readAllBytes, StandardCharsets.UTF_8))
    }

  extension (res: Resource.type) {

    def fromResources(path: String): Resource[IO, InputStream] =
      res.fromAutoCloseable(IO.defer {
        Option(getClass.getResourceAsStream(path))
          .orElse(Option(getClass.getClassLoader.getResourceAsStream(path)))
          .liftTo[IO](new Exception(s"Can't find $path"))
      })

    def fromResourcesAs[A](path: String)(fa: InputStream => IO[A]): IO[A] = fromResources(path).use(in => fa(in))
  }

  given [T]: Conversion[IO[T], Future[T]] =
    fun =>
      fun.unsafeToFuture()(cats.effect.unsafe.implicits.global)

  case class Request(params: Params, body: Option[Json])

  object Request {

    given Decoder[Request] =
      (c: HCursor) => {
        val params = c.downField("params").as[Map[String, String]].toOption
        val body   = c.downField("body").as[Json].toOption

        Right(Request(Params(params.getOrElse(Map.empty)), body))
      }

  }

  case class TestObject(
      request: Option[Request] = None,
      response: Json
  ) derives Decoder

  protected def parseJson[T: Decoder](input: String): IO[T] = IO.fromEither(parser.decode[T](input))

  protected def testRequest(url: String, method: Method, request: Option[String], params: Params): IO[String] =
    for {
      testObject  <- loadResource(s"$url/$method.json")
      requestBody <- request.traverse[IO, Json](parseJson(_))
      expected    <- parseJson[List[TestObject]](testObject)

      expectedResponse = expected.collectFirst { expected =>
        (expected, requestBody, params) match {

          case (TestObject(Some(Request(expectedParams, Some(expectedBody))), response), Some(body), params)
              if expectedParams == params && expectedBody == body =>
            response

          case (TestObject(Some(Request(expectedParams, None)), response), None, params)
              if expectedParams == params =>
            response
          case (TestObject(None, response), None, Params.Empty) => response
        }
      }.getOrElse(fail(
        s"Can't find combination of expected and generated request($request $params) and response $expected"
      ))
    } yield expectedResponse.noSpaces

}

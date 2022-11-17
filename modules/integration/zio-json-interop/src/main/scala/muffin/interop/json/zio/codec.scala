package muffin.interop.json.zio

import java.time.{Instant, LocalDateTime, ZoneId}
import scala.collection.mutable
import scala.deriving.Mirror
import scala.util.Right

import cats.Id

import zio.json.{*, given}
import zio.json.DeriveJsonDecoder.ArraySeq
import zio.json.JsonDecoder.{JsonError, UnsafeJson}
import zio.json.ast.{Json, JsonCursor}
import zio.json.internal.{Lexer, RetractReader, StringMatrix, Write}

import muffin.codec.*
import muffin.http.Body
import muffin.model.{Preference, RoleInfo}

object codec extends CodecSupport[JsonEncoder, JsonDecoder] {

  given EncodeTo[A: JsonEncoder]: Encode[A] = _.toJson

  given DecodeFrom[A: JsonDecoder]: Decode[A] = _.fromJson.left.map(new Throwable(_))

  given UnitTo: JsonEncoder[Unit] = (_, _, _) => ()

  given UnitFrom: JsonDecoder[Unit] = (_, _) => ()

  given StringTo: JsonEncoder[String] = JsonEncoder.string

  given StringFrom: JsonDecoder[String] = JsonDecoder.string

  given ListTo[A: JsonEncoder]: JsonEncoder[List[A]] = JsonEncoder.list[A]

  given ListFrom[A: JsonDecoder]: JsonDecoder[List[A]] = JsonDecoder.list[A]

  given MapFrom[A: JsonDecoder]: JsonDecoder[Map[String, A]] = JsonDecoder.map[String, A]

  given BoolTo: JsonEncoder[Boolean] = JsonEncoder.boolean

  given BoolFrom: JsonDecoder[Boolean] = JsonDecoder.boolean

  given LocalDateTimeTo(using zone: ZoneId): JsonEncoder[LocalDateTime] =
    JsonEncoder[Long].contramap[LocalDateTime](
      _.atZone(zone).toEpochSecond
    )

  given LocalDateTimeFrom(using zone: ZoneId): JsonDecoder[LocalDateTime] =
    JsonDecoder[Long].map[LocalDateTime](t =>
      LocalDateTime.ofInstant(Instant.ofEpochSecond(t), zone)
    )

  given LongTo: JsonEncoder[Long] = JsonEncoder.long

  given LongFrom: JsonDecoder[Long] = JsonDecoder.long

  given NothingTo: JsonEncoder[Nothing] = UnitTo.asInstanceOf[JsonEncoder[Nothing]]

  given NothingFrom: JsonDecoder[Nothing] = UnitFrom.asInstanceOf[JsonDecoder[Nothing]]

  given AnyTo: JsonEncoder[Any] = UnitTo.asInstanceOf[JsonEncoder[Any]]

  given AnyFrom: JsonDecoder[Any] = UnitFrom.asInstanceOf[JsonDecoder[Any]]

  given MapTo[K: JsonEncoder, V: JsonEncoder]: JsonEncoder[Map[K, V]] =
    (a: Map[K, V], indent: Option[Int], out: Write) => {
      given JsonFieldEncoder[K] = JsonEncoder[K].encodeJson(_, None).toString

      JsonEncoder.map[K, V].unsafeEncode(a, indent, out)
    }

  given OptionTo[A: JsonEncoder]: JsonEncoder[Option[A]] = JsonEncoder.option[A]

  given OptionFrom[A: JsonDecoder]: JsonDecoder[Option[A]] = JsonDecoder.option[A]

  def jsonRaw: JsonRequestRawBuilder[JsonEncoder, Body.RawJson] = new ZioBodyRawBuilder(Nil)

  def seal[T](f: T => JsonEncoder[T]): JsonEncoder[T] =
    (a: T, indent: Option[Int], out: Write) => f(a).unsafeEncode(a, indent, out)

  def json[T, X: JsonEncoder](f: T => X): JsonEncoder[T] =
    (a: T, indent: Option[Int], out: Write) => JsonEncoder[X].unsafeEncode(f(a), indent, out)

  def json[T]: JsonRequestBuilder[T, JsonEncoder] = new ZioJsonBuilder(Nil)

  def parsing[X: JsonDecoder, T](f: X => T): JsonDecoder[T] =
    (trace: List[JsonError], in: RetractReader) => f(JsonDecoder[X].unsafeDecode(trace, in))

  def parsing: JsonResponseBuilder[JsonDecoder, EmptyTuple] = new ZioResponseBuilder(Nil, Nil)

  private class ZioJsonBuilder[T](funs: List[T => String]) extends JsonRequestBuilder[T, JsonEncoder] {

    def single[X: JsonEncoder](value: X): String = value.toJson

    def field[X: JsonEncoder](fieldName: String, fieldValue: X): JsonRequestBuilder[T, JsonEncoder] = {
      val fun = (_: T) => s""""$fieldName":${fieldValue.toJson}"""

      ZioJsonBuilder(fun :: funs)
    }

    def field[X: JsonEncoder](fieldName: String, fieldValue: T => X): JsonRequestBuilder[T, JsonEncoder] = {
      val fun = (st: T) => s""""$fieldName": ${fieldValue(st).toJson}"""

      ZioJsonBuilder(fun :: funs)
    }

    def build: JsonEncoder[T] =
      (a: T, _: Option[Int], out: Write) => out.write(funs.map(_.apply(a.asInstanceOf[T])).mkString("{", ",", "}"))

  }

  private class ZioBodyRawBuilder(state: List[String]) extends JsonRequestRawBuilder[JsonEncoder, Body.RawJson] {

    def field[T: JsonEncoder](fieldName: String, fieldValue: T): JsonRequestRawBuilder[JsonEncoder, Body.RawJson] =
      ZioBodyRawBuilder(s""""$fieldName":${fieldValue.toJson}""" :: state)

    def build: Body.RawJson = Body.RawJson(state.mkString("{", ",", "}"))

  }

  private class ZioResponseBuilder[Params <: Tuple](stateNames: List[String], decoders: List[JsonDecoder[Any]])
    extends JsonResponseBuilder[JsonDecoder, Params] {

    override def field[X: JsonDecoder](name: String): JsonResponseBuilder[JsonDecoder, X *: Params] =
      new ZioResponseBuilder[X *: Params](
        name :: stateNames,
        summon[JsonDecoder[X]].asInstanceOf[JsonDecoder[Any]] :: decoders
      )

    override def build[X](f: PartialFunction[Params, X]): JsonDecoder[X] =
      new JsonDecoder[X] {

        val names: Array[String] = stateNames.toArray

        val len: Int = names.length

        val matrix: StringMatrix = new StringMatrix(names)

        val spans: Array[JsonError] = names.map(JsonError.ObjectAccess(_))

        lazy val tcs: Array[JsonDecoder[Any]] = decoders.toArray

        lazy val namesMap: Map[String, Int] = names.zipWithIndex.toMap

        def unsafeDecode(trace: List[JsonError], in: RetractReader): X = {
          Lexer.char(trace, in, '{')

          val ps: Array[Any] = Array.ofDim(len)

          if (Lexer.firstField(trace, in))
            while ({
              var trace_ = trace
              val field  = Lexer.field(trace, in, matrix)
              if (field != -1) {
                trace_ = spans(field) :: trace
                if (ps(field) != null)
                  throw UnsafeJson(JsonError.Message("duplicate") :: trace)
                ps(field) = tcs(field).unsafeDecode(trace_, in)
              } else
                Lexer.skipValue(trace_, in)

              Lexer.nextField(trace, in)
            })
              ()

          var i = 0
          while (i < len) {
            if (ps(i) == null) {
              tcs(i).unsafeDecodeMissing(spans(i) :: trace)
            }
            i += 1
          }

          val x = Tuple.fromArray(ps).asInstanceOf[Params]

          f(x)
        }

      }

  }

}

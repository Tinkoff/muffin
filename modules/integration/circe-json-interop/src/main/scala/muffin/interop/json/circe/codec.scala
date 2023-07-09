package muffin.interop.json.circe

import java.time.*
import scala.deriving.Mirror

import cats.{~>, catsParallelForId}
import cats.arrow.FunctionK
import cats.syntax.all.given

import io.circe.*
import io.circe.Decoder.Result
import io.circe.parser.*
import io.circe.syntax.given

import muffin.api.*
import muffin.codec.*
import muffin.error.MuffinError
import muffin.http.Body
import muffin.router.*

object codec extends CodecSupport[Encoder, Decoder] {

  given EncoderTo: FunctionK[Encoder, Encode] =
    new FunctionK[Encoder, Encode] {
      def apply[A](fa: Encoder[A]): Encode[A] = fa.apply(_).dropNullValues.noSpaces
    }

  given DecoderFrom: FunctionK[Decoder, Decode] =
    new FunctionK[Decoder, Decode] {

      def apply[A](fa: Decoder[A]): Decode[A] = decode[A](_)(fa).leftMap(err => MuffinError.Decoding(err.getMessage))

    }

  given EncodeTo[A: Encoder]: Encode[A] = Encoder[A].apply(_).dropNullValues.noSpaces

  given DecodeFrom[A: Decoder]: Decode[A] = decode[A](_).leftMap(err => MuffinError.Decoding(err.getMessage))

  given UnitTo: Encoder[Unit] = Encoder.encodeUnit

  given UnitFrom: Decoder[Unit] = _ => Right(())

  given StringTo: Encoder[String] = Encoder.encodeString

  given StringFrom: Decoder[String] = Decoder.decodeString

  given ListTo[A: Encoder]: Encoder[List[A]] = Encoder.encodeList[A]

  given ListFrom[A: Decoder]: Decoder[List[A]] = Decoder.decodeList[A]

  given MapFrom[A: Decoder]: Decoder[Map[String, A]] = Decoder.decodeMap[String, A]

  given BoolTo: io.circe.Encoder[Boolean] = Encoder.encodeBoolean

  given BoolFrom: io.circe.Decoder[Boolean] = Decoder.decodeBoolean

  given LocalDateTimeTo(using zone: ZoneId): io.circe.Encoder[LocalDateTime] =
    Encoder[Long].contramap[LocalDateTime](
      _.atZone(zone).toEpochSecond
    )

  given LocalDateTimeFrom(using zone: ZoneId): io.circe.Decoder[LocalDateTime] =
    Decoder[Long].map[LocalDateTime](t =>
      LocalDateTime.ofInstant(Instant.ofEpochSecond(t), zone)
    )

  given IntTo: io.circe.Encoder[Int] = Encoder.encodeInt

  given IntFrom: io.circe.Decoder[Int] = Decoder.decodeInt

  given LongTo: io.circe.Encoder[Long] = Encoder.encodeLong

  given LongFrom: io.circe.Decoder[Long] = Decoder.decodeLong

  given NothingTo: io.circe.Encoder[Nothing] = UnitTo.asInstanceOf[Encoder[Nothing]]

  given NothingFrom: io.circe.Decoder[Nothing] = UnitFrom.asInstanceOf[Decoder[Nothing]]

  given AnyTo: io.circe.Encoder[Any] = UnitTo.asInstanceOf[Encoder[Any]]

  given AnyFrom: io.circe.Decoder[Any] = UnitFrom.asInstanceOf[Decoder[Any]]

  given MapTo[K: Encoder, V: Encoder]: Encoder[Map[K, V]] =
    map => {
      given KeyEncoder[K] = Encoder[K].apply(_).noSpaces
      Encoder.encodeMap[K, V].apply(map)
    }

  given OptionTo[A: Encoder]: io.circe.Encoder[Option[A]] = Encoder.encodeOption[A]

  given OptionFrom[A: Decoder]: io.circe.Decoder[Option[A]] = Decoder.decodeOption[A]

  def jsonRaw: JsonRequestRawBuilder[Encoder, Body.RawJson] = new CirceBodyBuilder(JsonObject.empty)

  def seal[T](f: T => Encoder[T]): Encoder[T] = (a: T) => f(a).apply(a)

  def json[T, X: Encoder](f: T => X): Encoder[T] = (a: T) => Encoder[X].apply(f(a))

  def json[T]: JsonRequestBuilder[T, Encoder] = new CirceJsonBuilder(Nil)

  def parsing: JsonResponseBuilder[Decoder, EmptyTuple] = new CirceResponseBuilder[EmptyTuple](EmptyTupleFrom)

  private def EmptyTupleFrom: Decoder[EmptyTuple] = _ => Right(EmptyTuple)

  def parsing[X: Decoder, T](f: X => T): Decoder[T] =
    new Decoder[T] {
      override def apply(c: HCursor): Result[T] = Decoder[X].apply(c).map(f)
    }

  private class CirceJsonBuilder[T](funs: List[(T, JsonObject) => JsonObject]) extends JsonRequestBuilder[T, Encoder] {

    def field[X: Encoder](fieldName: String, fieldValue: T => X): JsonRequestBuilder[T, Encoder] = {
      val fun = (st: T, js: JsonObject) => js.add(fieldName, Encoder[X].apply(fieldValue(st)))
      CirceJsonBuilder(fun :: funs)
    }

    def rawField(fieldName: String, fieldValue: T => String): JsonRequestBuilder[T, Encoder] = {
      val fun = (st: T, js: JsonObject) => js.add(fieldName, parse(fieldValue(st)).getOrElse(Json.Null))
      CirceJsonBuilder(fun :: funs)
    }

    def build: Encoder[T] = (a: T) => Json.fromJsonObject(funs.foldLeft(JsonObject.empty)((acc, i) => i(a, acc)))
  }

  private class CirceBodyBuilder(state: JsonObject) extends JsonRequestRawBuilder[Encoder, Body.RawJson] {

    def field[T: Encoder](fieldName: String, fieldValue: T): JsonRequestRawBuilder[Encoder, Body.RawJson] =
      CirceBodyBuilder(state.add(fieldName, fieldValue.asJson))

    def build: Body.RawJson = Body.RawJson(Json.fromJsonObject(state).dropNullValues.noSpaces)

  }

  private class CirceResponseBuilder[Decoders <: Tuple](decoders: Decoder[Decoders])
    extends JsonResponseBuilder[Decoder, Decoders] {

    def field[X: Decoder](name: String): JsonResponseBuilder[Decoder, X *: Decoders] =
      CirceResponseBuilder[X *: Decoders](decoders.flatMap(all => Decoder[X].at(name).map(_ *: all)))

    def rawField(name: String): JsonResponseBuilder[Decoder, Option[String] *: Decoders] =
      CirceResponseBuilder[Option[String] *: Decoders] {
        decoders.flatMap(all =>
          Decoder[Option[Json]].at(name).map(_.map(_.noSpaces) *: all)
        )
      }

    def internal[X: Decoder](name: String): JsonResponseBuilder[Decoder, X *: Decoders] =
      CirceResponseBuilder[X *: Decoders](
        decoders.flatMap(all =>
          Decoder[String].at(name).flatMap(s =>
            (_: HCursor) =>
              decode[X](s).left.map {
                case ParsingFailure(message, _) => DecodingFailure(message, Nil)
                case failure: DecodingFailure   => failure
              }
          ).map(_ *: all)
        )
      )

    def select[X](f: PartialFunction[Decoders, Decoder[X]]): Decoder[X] =
      (c: HCursor) => decoders.apply(c).map(f).flatMap(_.apply(c))

    def build[X](f: PartialFunction[Decoders, X]): Decoder[X] = (c: HCursor) => decoders.apply(c).map(f)

  }

}

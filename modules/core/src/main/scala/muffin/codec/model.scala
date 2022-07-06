package muffin.codec

import muffin.http.Body

trait Encode[A] {
  def apply(obj: A): String
}

trait RawDecode[R, T] {
  def apply(from: R): Either[Throwable, T]
}

type Decode[T] = RawDecode[String, T]

trait JsonRequestBuilder[To[_]](){
  def field[T: To](fieldName: String, fieldValue: T): JsonRequestBuilder[To]

  def field[T: To](fieldName: String, fieldValue: Option[T]): JsonRequestBuilder[To]

  def build: Body.RawJson
}


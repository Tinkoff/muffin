package muffin.codec

import muffin.http.Body

trait Encode[A] {
  def apply(obj: A): String
}

trait RawDecode[R, T] {
  def apply(from: R): Either[Throwable, T]
}

type Decode[T] = RawDecode[String, T]

trait JsonRequestBuilder[To[_], R](){
  def field[T: To](fieldName: String, fieldValue: T): JsonRequestBuilder[To, R]

  def build: Body.Json[R]
}


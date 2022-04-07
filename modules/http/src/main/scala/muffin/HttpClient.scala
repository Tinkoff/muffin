package muffin

import io.circe.{Decoder, Encoder}
import io.circe.syntax.given
import java.io.File

trait HttpClient[F[_]] {
  def request[In, Out: Decoder](
    url: String,
    method: Method,
    body: Body[In] = Body.Empty,
    headers: Map[String, String] = Map.empty
  ): F[Out]
}

sealed trait Body[+T]

object Body:
  case object Empty extends Body[Nothing]

  case class Json[T: Encoder](value: T) extends Body[T] {
    def as: String = value.asJson.dropNullValues.noSpaces
  }

  case class Multipart(parts: List[MultipartElement]) extends Body[Nothing]

sealed trait MultipartElement
object MultipartElement {
  case class StringElement(name: String, value: String) extends MultipartElement
  case class FileElement(name: String, value: File) extends MultipartElement
}

enum Method:
  case Get, Post, Delete

package muffin.http

import io.circe.parser.parse
import io.circe.syntax.given
import io.circe.{Decoder, Encoder}

import java.io.File
import muffin.codec.*

trait HttpClient[F[_], To[_], From[_]] {
  def request[In: To,  Out: From](
    url: String,
    method: Method,
    body: Body[In] = Body.Empty,
    headers: Map[String, String] = Map.empty
  ): F[Out]
}

sealed trait Body[+T]

object Body:
  case object Empty extends Body[Nothing]

  case class Json[T](value: T) extends Body[T]

  case class Multipart(parts: List[MultipartElement]) extends Body[Nothing]

sealed trait MultipartElement
object MultipartElement {
  case class StringElement(name: String, value: String) extends MultipartElement
  case class FileElement(name: String, value: File) extends MultipartElement
}

enum Method:
  case Get, Post, Delete, Put, Patch

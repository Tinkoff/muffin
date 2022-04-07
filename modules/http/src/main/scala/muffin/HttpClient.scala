package muffin

import io.circe.{Decoder, Encoder}
import io.circe.syntax.given

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

enum Method:
  case Get, Post, Delete

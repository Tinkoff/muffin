package muffin.error

sealed trait MuffinError(message: String) extends Throwable

object MuffinError {

  case class Decoding(message: String) extends MuffinError(message)

  case class Http(message: String) extends MuffinError(message)
}

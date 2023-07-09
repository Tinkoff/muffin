package muffin.error

sealed abstract class MuffinError(message: String) extends Throwable(message)

object MuffinError {

  case class Decoding(message: String) extends MuffinError(message)

  case class Http(message: String) extends MuffinError(message)
}

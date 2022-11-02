package muffin.model

enum ResponseType {

  case Ephemeral

  case InChannel

}

sealed trait AppResponse[+T]

object AppResponse {

  case class Ok() extends AppResponse[Nothing]

  case class Message[T](responseType: ResponseType, text: Option[String] = None, attachments: List[Attachment[T]] = Nil)
    extends AppResponse[T]

  case class Errors[T](errors: Map[String, String]) extends AppResponse[T]

}

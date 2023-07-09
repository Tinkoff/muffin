package muffin.model

enum ResponseType {
  case Ephemeral
  case InChannel
}

sealed trait AppResponse

object AppResponse {
  case class Ok() extends AppResponse
  case class Errors(errors: Map[String, String]) extends AppResponse

  case class Message(
      responseType: ResponseType,
      text: Option[String] = None,
      attachments: List[Attachment] = Nil
  ) extends AppResponse

}

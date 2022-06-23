package muffin.predef

import io.circe.{Decoder, Encoder, Json}

opaque type Login = String

object Login {
  def apply(login: String): Login = login
}

opaque type UserId = String

object UserId {
  def apply(id: String): UserId = id
}

opaque type GroupId = String

object GroupId {
  def apply(id: String): GroupId = id
}

opaque type TeamId = String

object TeamId {
  def apply(id: String): TeamId = id
}

opaque type ChannelId = String

object ChannelId {
  def apply(d: String): ChannelId = d
}

opaque type MessageId = String

object MessageId {
  def apply(id: String): MessageId = id
}

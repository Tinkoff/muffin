package muffin.predef

import io.circe.{Decoder, Encoder, Json}

opaque type Login = String

object Login {
  def apply(login: String): Login = login

  given encoder: Encoder[Login] = Encoder.encodeString.contramap(identity)
  given decoder: Decoder[Login] = Decoder.decodeString.map(identity)
}

opaque type UserId = String

object UserId {
  def apply(id: String): UserId = id

  given encoder: Encoder[UserId] = Encoder.encodeString.contramap(identity)
  given decoder: Decoder[UserId] = Decoder.decodeString.map(identity)
}

opaque type GroupId = String

object GroupId {
  def apply(id: String): GroupId = id

  given encoder: Encoder[GroupId] = Encoder.encodeString.contramap(identity)
  given decoder: Decoder[GroupId] = Decoder.decodeString.map(identity)
}

opaque type TeamId = String

object TeamId {
  def apply(id: String): TeamId = id

  given encoder: Encoder[TeamId] = Encoder.encodeString.contramap(identity)
  given decoder: Decoder[TeamId] = Decoder.decodeString.map(identity)
}

opaque type ChannelId = String

object ChannelId {
  def apply(d: String): ChannelId = d

  given encoder: Encoder[ChannelId] = Encoder.encodeString.contramap(identity)
  given decoder: Decoder[ChannelId] = Decoder.decodeString.map(identity)
}


opaque type MessageId = String

object MessageId {
  def apply(id: String): MessageId = id
  given encoder: Encoder[MessageId] = Encoder.encodeString.contramap(identity)
  given decoder: Decoder[MessageId] = Decoder.decodeString.map(identity)
}

package muffin.predef

import io.circe.{Decoder, Encoder, Json}

opaque type Login = String

opaque type UserId = String

object UserId {
  def apply(id: String): UserId = id

  given encoder: Encoder[UserId] = Encoder.encodeString.contramap(identity)
  given decoder: Decoder[UserId] = Decoder.decodeString.map(identity)
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

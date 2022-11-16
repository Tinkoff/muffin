package muffin.model

import cats.Show

trait ApplyOpaque[T, U <: T] {
  def apply(value: U): T = value
}

trait StringShowOpaque[U <: String] {
  given Show[U] = identity(_)
}

opaque type Login = String
object Login extends ApplyOpaque[Login, String] with StringShowOpaque[Login]

opaque type UserId = String
object UserId extends ApplyOpaque[UserId, String] with StringShowOpaque[UserId]

opaque type GroupId = String
object GroupId extends ApplyOpaque[GroupId, String] with StringShowOpaque[GroupId]

opaque type TeamId = String
object TeamId extends ApplyOpaque[TeamId, String] with StringShowOpaque[TeamId]

opaque type ChannelId = String
object ChannelId extends ApplyOpaque[ChannelId, String] with StringShowOpaque[ChannelId]

opaque type MessageId = String
object MessageId extends ApplyOpaque[MessageId, String] with StringShowOpaque[MessageId]

package muffin.channels

import fs2.Stream

import io.circe.Decoder
import muffin.predef.*


trait Channels[F[_]] {
  def members(channelId: ChannelId): Stream[F, ChannelMember]

  def direct(userIds: List[UserId]): F[ChannelInfo]

  def getChannelByName(teamId: TeamId, name: String): F[ChannelInfo]
}

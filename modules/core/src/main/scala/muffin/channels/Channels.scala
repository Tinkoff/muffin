package muffin.channels

import fs2.Stream

import io.circe.Decoder
import muffin.predef.*


trait Channels[F[_]] {

  def members(req: MembersRequest): F[List[ChannelMember]]

//  def members(channelId: ChannelId): F[Stream[F, ChannelMember]]

  def direct(req: CreateDirectChannelRequest): F[ChannelInfo]

  def getChannelByName(team: String, name: String): F[ChannelInfo]
}

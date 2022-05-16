package muffin.channels

import io.circe.Decoder
import muffin.predef.*

trait Channels[F[_]] {

  def members(req: MembersRequest): F[List[ChannelMember]]

  def direct(req: CreateDirectChannelRequest): F[ChannelInfo]
}

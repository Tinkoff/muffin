package muffin.channels
import io.circe.Decoder

import muffin.predef.*

case class MembersRequest(
  channelId: ChannelId,
  page: Option[Int],
  per_page: Option[Int]
)

case class ChannelMember(
  channelId: ChannelId,
  userId: UserId,
  roles: String,
  last_viewed_at: Long, // TODO timestamp
  msgCount: Option[Int],
  mentionCount: Option[Int],
  //                          notifyProps:
  lastUpdateAt: Option[Long] // TODO timestamp
) derives Decoder

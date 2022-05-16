package muffin.channels
import io.circe.{Codec, Decoder}
import muffin.predef.*

case class MembersRequest(
  channelId: ChannelId,
  page: Option[Int],
  per_page: Option[Int]
)

case class ChannelMember(
  channel_id: ChannelId,
  user_id: UserId,
  roles: String,
  last_viewed_at: Long, // TODO timestamp
  msg_count: Option[Int],
  mention_count: Option[Int],
  //                          notifyProps:
  last_update_at: Option[Long] // TODO timestamp
) derives Decoder

type CreateDirectChannelRequest = List[UserId]

case class ChannelInfo(id: ChannelId) derives Codec.AsObject

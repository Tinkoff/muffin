package muffin.status

import muffin.predef.*

import java.time.LocalDateTime

case class UserStatus(
   userId: UserId,
   status: StatusUser,
   manual: Boolean,
   lastActivityAt: LocalDateTime,
   activeChannel: Option[ChannelId],
)


enum StatusUser:
  case Online
  case Offline
  case Away
  case Dnd(time: LocalDateTime)


case class CustomStatus(
  emoji: String,
  text: Option[String],
  duration: Option[CustomStatusDuration]
)

enum CustomStatusDuration:
  case ThirtyMinutes
  case OneHour
  case FourHours
  case Today
  case ThisWeek
  case DateAndTime(duration: LocalDateTime)





case class UpdateUserStatusRequest(userId: UserId, status: StatusUser)
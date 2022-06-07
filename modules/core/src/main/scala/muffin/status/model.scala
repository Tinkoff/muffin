package muffin.status

import muffin.predef.*

import java.time.LocalDateTime

case class UserStatus(
                       user_id: UserId,
                       status: Status,
                       manual: Boolean,
                       last_activity_at: Long, // TO DO timestamp,
                       active_channel: Option[ChannelId],
                     )


enum StatusUser:
  case Online
  case Offline
  case Away
  case Dnd(time: Int) // TO DO timestamp


case class CustomStatus(
                         emoji: String,
                         text: Option[String],
                         duration: Option[CustomStatusDuration]
                       )

enum CustomStatusDuration:
  case thirty_minutes
  case thirty_minutes
  case one_hour
  case four_hours
  case today
  case this_week
  case date_and_time(duration: LocalDateTime)
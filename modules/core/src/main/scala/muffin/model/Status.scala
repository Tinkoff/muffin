package muffin.model

import java.time.LocalDateTime

import cats.syntax.all.given

case class UserStatus(
    userId: UserId,
    status: StatusUser,
    manual: Boolean,
    lastActivityAt: LocalDateTime,
    activeChannel: Option[ChannelId]
)

enum StatusUser {
  case Online
  case Offline
  case Away
  case Dnd(time: LocalDateTime)
}

enum RawStatusUser {
  case Online
  case Offline
  case Away
  case Dnd
}

object StatusUser {

  def apply(raw: RawStatusUser, time: Option[LocalDateTime]): Option[StatusUser] =
    (raw, time) match {
      case (RawStatusUser.Dnd, Some(time)) => StatusUser.Dnd(time).some
      case (RawStatusUser.Away, _)         => StatusUser.Away.some
      case (RawStatusUser.Online, _)       => StatusUser.Online.some
      case (RawStatusUser.Offline, _)      => StatusUser.Offline.some
      case _                               => None
    }

}

case class CustomStatus(emoji: String, text: Option[String], duration: Option[CustomStatusDuration])

enum RawCustomStatusDuration {
  case ThirtyMinutes
  case OneHour
  case FourHours
  case Today
  case ThisWeek
  case DateAndTime
}

enum CustomStatusDuration {
  case ThirtyMinutes
  case OneHour
  case FourHours
  case Today
  case ThisWeek
  case DateAndTime(duration: LocalDateTime)
}

object CustomStatusDuration {

  def apply(raw: RawCustomStatusDuration, time: Option[LocalDateTime]): Option[CustomStatusDuration] =
    (raw, time) match {
      case (RawCustomStatusDuration.DateAndTime, Some(time)) => CustomStatusDuration.DateAndTime(time).some
      case (RawCustomStatusDuration.ThisWeek, _)             => CustomStatusDuration.ThisWeek.some
      case (RawCustomStatusDuration.Today, _)                => CustomStatusDuration.Today.some
      case (RawCustomStatusDuration.FourHours, _)            => CustomStatusDuration.FourHours.some
      case (RawCustomStatusDuration.OneHour, _)              => CustomStatusDuration.OneHour.some
      case (RawCustomStatusDuration.ThirtyMinutes, _)        => CustomStatusDuration.ThirtyMinutes.some
      case _                                                 => None
    }

}

case class UpdateUserStatusRequest(userId: UserId, status: StatusUser)

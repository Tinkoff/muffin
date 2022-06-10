package muffin.codec

import muffin.insights.*
import muffin.status.*
import muffin.preferences.*
import muffin.roles.RoleInfo

import java.time.*


trait Encode[A] {
  def apply(obj: A): String
}

trait RawDecode[R, T] {
  def apply(from: R): Either[Throwable, T]
}

type Decode[T] = RawDecode[String, T]


trait MuffinCodec[R, To[_], From[_]] {
  given CirceTo[A: io.circe.Encoder]: To[A]

  given CirceFrom[A: io.circe.Decoder]: From[A]

  given RawFrom[T: From]: RawDecode[R, T]
  
  given EncodeTo[A: To]: Encode[A]

  given DecodeFrom[A: From]: Decode[A]

  given ListTo[A: To]: To[List[A]]

  given ListFrom[A: From]: From[List[A]]

  given UnitFrom: From[Unit]

  //Preferences
  given PreferenceEncode: To[Preference]

  given PreferenceDecode: From[Preference]
  //Preferences

  //Status
  given StatusUserEncode: To[StatusUser]

  given StatusUserDecode(using time: Option[LocalDateTime]): From[StatusUser]

  given UserStatusEncode(using zone: ZoneId): To[UserStatus]

  given UserStatusDecode(using zone: ZoneId): From[UserStatus]

  given CustomStatusDurationEncode: To[CustomStatusDuration]

  given CustomStatusDurationDecode(using time: Option[LocalDateTime]): From[CustomStatusDuration]

  given CustomStatusEncode(using zone: ZoneId): To[CustomStatus]

  given CustomStatusDecode(using zone: ZoneId): From[CustomStatus]

  given UpdateUserStatusRequestEncode(using zone: ZoneId): To[UpdateUserStatusRequest]
  //Status

  //Insights
  given ReactionInsightDecode: From[ReactionInsight]

  given ChannelInsightDecode: From[ChannelInsight]

  given ListWrapperDecode[T: From]: From[ListWrapper[T]]
  //Insights

  //Roles
  given RoleInfoDecode: From[RoleInfo]
  //Roles
}
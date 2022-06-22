package muffin.codec

import muffin.api.channels.*
import muffin.api.emoji.*
import muffin.api.insights.*
import muffin.api.preferences.*
import muffin.api.roles.*
import muffin.api.status.*
import muffin.api.dialogs.*
import muffin.input.*

import java.time.*

trait CodecSupport[R, To[_], From[_]] {
  def json: JsonRequestBuilder[To, R]

  given CirceTo[A: io.circe.Encoder]: To[A]

  given CirceFrom[A: io.circe.Decoder]: From[A]

  given RawFrom[T: From]: RawDecode[R, T]

  given EncodeTo[A: To]: Encode[A]

  given DecodeFrom[A: From]: Decode[A]

  given ListTo[A: To]: To[List[A]]

  given ListFrom[A: From]: From[List[A]]

  given UnitFrom: From[Unit]

  given RTo: To[R]

  //Channels
  given NotifyOptionFrom: From[NotifyOption]
  
  given UnreadOptionFrom: From[UnreadOption]
  
  given NotifyPropsFrom: From[NotifyProps]
  
  given ChannelMemberFrom: From[ChannelMember]
  
  given ChannelInfoFrom: From[ChannelInfo]
  //Channels

  //Emoji
  given EmojiInfoFrom: From[EmojiInfo]
  //Emoji

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


  given DialogEncode: To[Dialog]


  //  input

  given CommandContextFrom: From[CommandContext]

  given DialogContextFrom: From[DialogContext]

  given RawActionFrom: From[RawAction[R]]

  given AppResponseTo: To[AppResponse]

  //  input

}
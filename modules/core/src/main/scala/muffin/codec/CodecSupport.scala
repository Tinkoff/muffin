package muffin.codec

import muffin.api.channels.*
import muffin.api.emoji.*
import muffin.api.insights.*
import muffin.api.preferences.*
import muffin.api.roles.*
import muffin.api.status.*
import muffin.api.dialogs.*
import muffin.api.reactions.*
import muffin.api.users.*
import muffin.api.posts.*
import muffin.input.*
import muffin.predef.*

import java.time.*

trait CodecSupport[R, To[_], From[_]] {
  def json: JsonRequestBuilder[To, R]

  given RawFrom[T: From]: RawDecode[R, T]

  given EncodeTo[A: To]: Encode[A]

  given DecodeFrom[A: From]: Decode[A]

  given StringTo: To[String]

  given ListTo[A: To]: To[List[A]]

  given ListFrom[A: From]: From[List[A]]

  given MapFrom[A: From]: From[Map[String, A]]

  given UnitFrom: From[Unit]

  given RTo: To[R]

  given LoginTo: To[Login]
  given UserIdTo: To[UserId]
  given GroupIdTo: To[GroupId]
  given TeamIdTo: To[TeamId]
  given ChannelIdTo: To[ChannelId]
  given MessageIdTo: To[MessageId]
  given EmojiIdTo: To[EmojiId]

  given LoginFrom: From[Login]
  given UserIdFrom: From[UserId]
  given GroupIdFrom: From[GroupId]
  given TeamIdFrom: From[TeamId]
  given ChannelIdFrom: From[ChannelId]
  given MessageIdFrom: From[MessageId]
  given EmojiIdFrom: From[EmojiId]


  //Channels
  given NotifyOptionFrom: From[NotifyOption]
  
  given UnreadOptionFrom: From[UnreadOption]
  
  given NotifyPropsFrom: From[NotifyProps]
  
  given ChannelMemberFrom(using zone: ZoneId): From[ChannelMember]
  
  given ChannelInfoFrom(using zone: ZoneId): From[ChannelInfo]
  //Channels

  //Emoji
  given EmojiInfoFrom(using zone: ZoneId): From[EmojiInfo]
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


  given DialogTo: To[Dialog]

  given DialogElementTo: To[Element]

  given DataSourceTo: To[DataSource]

  given SelectOptionTo: To[SelectOption]

  given TextSubtypeTo: To[TextSubtype]


  given ReactionInfoFrom(using zone: ZoneId): From[ReactionInfo]

  //  input

  given CommandContextFrom: From[CommandContext]

  given DialogContextFrom: From[DialogContext]

  given DialogSubmissionValueFrom: From[DialogSubmissionValue]
  
  given RawActionFrom: From[RawAction[R]]

  given AppResponseTo: To[AppResponse]

  given ResponseTypeTo: To[ResponseType]

  //  input

  given UserFrom(using zone: ZoneId): From[User]




  given ActionTo: To[Action]
  given ActionFrom: From[Action]

  given IntegrationTo: To[Integration]
  given StyleTo: To[Style]
  given StyleFrom: From[Style]
  given PropsTo: To[Props]
  given AttachmentTo: To[Attachment]
  given AttachmentFieldTo: To[AttachmentField]


  given PostFrom: From[Post]


}
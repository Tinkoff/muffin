//package muffin.interop.zio.json
//
//import muffin.codec.{CodecSupport, JsonRequestBuilder}
//import muffin.http.Body
//import zio.json.{JsonDecoder, JsonEncoder}
//import zio.json.ast.Json
//
//object codec extends CodecSupport[Json, JsonEncoder, JsonDecoder] {
//  def json: JsonRequestBuilder[To, R]
//
//  given RawFrom[T: JsonDecoder]: RawDecode[R, T]
//
//  given EncodeTo[A: JsonEncoder]: Encode[A]
//
//  given DecodeFrom[A: JsonDecoder]: Decode[A]
//
//  given StringTo: JsonEncoder[String]
//
//  given ListTo[A: JsonEncoder]: JsonEncoder[List[A]]
//
//  given ListFrom[A: JsonDecoder]: JsonDecoder[List[A]]
//
//  given MapFrom[A: JsonDecoder]: JsonDecoder[Map[String, A]]
//
//  given UnitTo: JsonEncoder[Unit]
//
//  given UnitFrom: JsonDecoder[Unit]
//
//  given RTo: JsonEncoder[R]
//
//  given LoginTo: JsonEncoder[Login] = JsonEncoder.string.contramap(_.toString)
//
//  given UserIdTo: JsonEncoder[UserId] = JsonEncoder.string.contramap(_.toString)
//
//  given GroupIdTo: JsonEncoder[GroupId] = JsonEncoder.string.contramap(_.toString)
//
//  given TeamIdTo: JsonEncoder[TeamId] = JsonEncoder.string.contramap(_.toString)
//
//  given ChannelIdTo: JsonEncoder[ChannelId] = JsonEncoder.string.contramap(_.toString)
//
//  given MessageIdTo: JsonEncoder[MessageId] = JsonEncoder.string.contramap(_.toString)
//
//  given EmojiIdTo: JsonEncoder[EmojiId] = JsonEncoder.string.contramap(_.toString)
//
//  given LoginFrom: JsonDecoder[Login] = JsonDecoder.string.map(Login(_))
//
//  given UserIdFrom: JsonDecoder[UserId] = JsonDecoder.string.map(UserId(_))
//
//  given GroupIdFrom: JsonDecoder[GroupId] = JsonDecoder.string.map(GroupId(_))
//
//  given TeamIdFrom: JsonDecoder[TeamId] = JsonDecoder.string.map(TeamId(_))
//
//  given ChannelIdFrom: JsonDecoder[ChannelId] = JsonDecoder.string.map(ChannelId(_))
//
//  given MessageIdFrom: JsonDecoder[MessageId] = JsonDecoder.string.map(MessageId(_))
//
//  given EmojiIdFrom: JsonDecoder[EmojiId] = JsonDecoder.string.map(EmojiId(_))
//
//  //Channels
//  given NotifyOptionFrom: JsonDecoder[NotifyOption]
//
//  given UnreadOptionFrom: JsonDecoder[UnreadOption]
//
//  given NotifyPropsFrom: JsonDecoder[NotifyProps]
//
//  given ChannelMemberFrom(using zone: ZoneId): JsonDecoder[ChannelMember]
//
//  given ChannelInfoFrom(using zone: ZoneId): JsonDecoder[ChannelInfo]
//  //Channels
//
//  //Emoji
//  given EmojiInfoFrom(using zone: ZoneId): JsonDecoder[EmojiInfo]
//  //Emoji
//
//  //Preferences
//  given PreferenceEncode: JsonEncoder[Preference]
//
//  given PreferenceDecode: JsonDecoder[Preference]
//  //Preferences
//
//  //Status
//  given StatusUserEncode: JsonEncoder[StatusUser]
//
//  given StatusUserDecode(using time: Option[LocalDateTime]): JsonDecoder[StatusUser]
//
//  given UserStatusEncode(using zone: ZoneId): JsonEncoder[UserStatus]
//
//  given UserStatusDecode(using zone: ZoneId): JsonDecoder[UserStatus]
//
//  given CustomStatusDurationEncode: JsonEncoder[CustomStatusDuration]
//
//  given CustomStatusDurationDecode(using time: Option[LocalDateTime]): JsonDecoder[CustomStatusDuration]
//
//  given CustomStatusEncode(using zone: ZoneId): JsonEncoder[CustomStatus]
//
//  given CustomStatusDecode(using zone: ZoneId): JsonDecoder[CustomStatus]
//
//  given UpdateUserStatusRequestEncode(using zone: ZoneId): JsonEncoder[UpdateUserStatusRequest]
//  //Status
//
//  //Insights
//  given ReactionInsightDecode: JsonDecoder[ReactionInsight]
//
//  given ChannelInsightDecode: JsonDecoder[ChannelInsight]
//
//  given ListWrapperDecode[T: JsonDecoder]: JsonDecoder[ListWrapper[T]]
//  //Insights
//
//  //Roles
//  given RoleInfoDecode: JsonDecoder[RoleInfo]
//  //Roles
//
//
//  given DialogTo: JsonEncoder[Dialog]
//
//  given DialogElementTo: JsonEncoder[Element]
//
//  given DataSourceTo: JsonEncoder[DataSource]
//
//  given SelectOptionTo: JsonEncoder[SelectOption]
//
//  given TextSubtypeTo: JsonEncoder[TextSubtype]
//
//
//  given ReactionInfoFrom(using zone: ZoneId): JsonDecoder[ReactionInfo]
//
//  //  input
//  given DialogContextFrom: JsonDecoder[DialogContext]
//
//  given DialogSubmissionValueFrom: JsonDecoder[DialogSubmissionValue]
//
//  given RawActionFrom: JsonDecoder[RawAction[R]]
//
//  given AppResponseTo: JsonEncoder[AppResponse]
//
//  given ResponseTypeTo: JsonEncoder[ResponseType]
//
//  //  input
//
//  given UserFrom(using zone: ZoneId): JsonDecoder[User]
//
//
//  given ActionTo: JsonEncoder[Action]
//
//  given ActionFrom: JsonDecoder[Action]
//
//  given IntegrationTo: JsonEncoder[Integration]
//
//  given StyleTo: JsonEncoder[Style]
//
//  given StyleFrom: JsonDecoder[Style]
//
//  given PropsTo: JsonEncoder[Props]
//
//  given AttachmentTo: JsonEncoder[Attachment]
//
//  given AttachmentFieldTo: JsonEncoder[AttachmentField]
//
//  given PostFrom: JsonDecoder[Post]
//
//
//
//  private class ZioJsonBuilder(json: Json) extends JsonRequestBuilder[JsonEncoder]{
//    override def field[T: JsonEncoder](fieldName: String, fieldValue: T): JsonRequestBuilder[JsonEncoder] = {
//
//      json.merge(Json.Obj(fieldName -> JsonEncoder[T].toJson))
//
//
//
//    }
//
//    override def field[T: JsonEncoder](fieldName: String, fieldValue: Option[T]): JsonRequestBuilder[JsonEncoder] = ???
//
//    override def build: Body.RawJson = ???
//  }
//}

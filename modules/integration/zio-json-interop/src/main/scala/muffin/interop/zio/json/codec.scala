package muffin.interop.zio.json

import muffin.codec.{CodecSupport, JsonRequestBuilder}
import zio.json.{JsonDecoder, JsonEncoder}
import zio.json.ast.Json

object codec extends CodecSupport[Json, JsonEncoder, JsonDecoder] {
  given ChannelInfoFrom: JsonDecoder[muffin.api.channels.ChannelInfo] = ???

  given ChannelInsightDecode: JsonDecoder[muffin.api.insights.ChannelInsight] = ???

  given ChannelMemberFrom: JsonDecoder[muffin.api.channels.ChannelMember] = ???

  given CirceFrom[A](using io.circe.Decoder[A]): JsonDecoder[A] = ???

  given CirceTo[A](using evidence$1: io.circe.Encoder[A]): JsonEncoder[A] = ???

  given CustomStatusDecode(using zone: java.time.ZoneId): JsonDecoder[muffin.api.status.CustomStatus] = ???

  given CustomStatusDurationDecode(using time: Option[java.time.LocalDateTime]): JsonDecoder[muffin.api.status.CustomStatusDuration] = ???

  given CustomStatusDurationEncode: JsonEncoder[muffin.api.status.CustomStatusDuration] = ???

  given CustomStatusEncode(using zone: java.time.ZoneId): JsonEncoder[muffin.api.status.CustomStatus] = ???

  given DecodeFrom[A](using evidence$5: JsonDecoder[A]): muffin.codec.Decode[A] = ???

  given DialogEncode: JsonEncoder[muffin.api.dialogs.Dialog] = ???

  given EmojiInfoFrom: JsonDecoder[muffin.api.emoji.EmojiInfo] = ???

  given EncodeTo[A](using evidence$4: JsonEncoder[A]): muffin.codec.Encode[A] = ???

  given ListFrom[A](using evidence$7: JsonDecoder[A]): JsonDecoder[List[A]] = ???

  given ListTo[A](using evidence$6: JsonEncoder[A]): JsonEncoder[List[A]] = ???

  given ListWrapperDecode[T](using evidence$8: JsonDecoder[T]): JsonDecoder[muffin.api.insights.ListWrapper[T]] = ???

  given NotifyOptionFrom: JsonDecoder[muffin.api.channels.NotifyOption] = ???

  given NotifyPropsFrom: JsonDecoder[muffin.api.channels.NotifyProps] = ???

  given PreferenceDecode: JsonDecoder[muffin.api.preferences.Preference] = ???

  given PreferenceEncode: JsonEncoder[muffin.api.preferences.Preference] = ???

  given RTo: JsonEncoder[Json] = ???

  given RawFrom[T](using evidence$3: JsonDecoder[T]): muffin.codec.RawDecode[Json, T] = ???

  given ReactionInsightDecode: JsonDecoder[muffin.api.insights.ReactionInsight] = ???

  given RoleInfoDecode: JsonDecoder[muffin.api.roles.RoleInfo] = ???

  given StatusUserDecode(using time: Option[java.time.LocalDateTime]): JsonDecoder[muffin.api.status.StatusUser] = ???

  given StatusUserEncode: JsonEncoder[muffin.api.status.StatusUser] = ???

  given UnitFrom: JsonDecoder[Unit] = ???

  given UnreadOptionFrom: JsonDecoder[muffin.api.channels.UnreadOption] = ???

  given UpdateUserStatusRequestEncode(using zone: java.time.ZoneId): JsonEncoder[muffin.api.status.UpdateUserStatusRequest] = ???

  given UserStatusDecode(using zone: java.time.ZoneId): JsonDecoder[muffin.api.status.UserStatus] = ???

  given UserStatusEncode(using zone: java.time.ZoneId): JsonEncoder[muffin.api.status.UserStatus] = ???

  def json: JsonRequestBuilder[JsonEncoder, Json] = ???

  given AppResponseTo: JsonEncoder[muffin.input.AppResponse] = ???
  given CommandContextFrom: JsonDecoder[muffin.input.CommandContext] = ???
  given DialogContextFrom: JsonDecoder[muffin.input.DialogContext] = ???
  given RawActionFrom: JsonDecoder[muffin.input.RawAction[zio.json.ast.Json]] = ???
}

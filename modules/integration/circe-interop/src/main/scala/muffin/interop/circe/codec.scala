package muffin.interop.circe

import cats.arrow.FunctionK
import cats.~>
import muffin.codec.*
import io.circe.*
import io.circe.parser.*
import muffin.preferences.*
import muffin.status.*
import muffin.predef.*
import muffin.insights.*
import cats.syntax.all.given
import io.circe.Decoder.Result
import muffin.roles.RoleInfo

import java.time.*


object codec extends MuffinCodec[Json, Encoder, Decoder] {
  given RawFrom[T: Decoder]: RawDecode[Json, T] = (from: Json) => from.as[T]

  given EncoderTo: FunctionK[Encoder, Encode] = new FunctionK[Encoder, Encode] {
    def apply[A](fa: Encoder[A]): Encode[A] = (obj: A) => fa.apply(obj).dropNullValues.noSpaces
  }

  given DecoderFrom: FunctionK[Decoder, Decode] = new FunctionK[Decoder, Decode] {
    def apply[A](fa: Decoder[A]): Decode[A] = (str: String) => parse(str).flatMap(_.as(fa))
  }

  given EncodeTo[A: Encoder]: Encode[A] = Encoder[A].apply(_).dropNullValues.noSpaces

  given DecodeFrom[A: Decoder]: Decode[A] =  (str: String) => parse(str).flatMap(_.as[A])

  given UnitFrom: Decoder[Unit] = Decoder.decodeUnit

  given CirceTo[A: Encoder]: Encoder[A] = summon[Encoder[A]]

  given CirceFrom[A: Decoder]: Decoder[A] = summon[Decoder[A]]

  given ListTo[A: Encoder]: Encoder[List[A]] = Encoder.encodeList[A]

  given ListFrom[A: Decoder]: Decoder[List[A]] = Decoder.decodeList[A]

  given PreferenceEncode: Encoder[Preference] = p =>
    Json.obj(
      "user_id" -> Json.fromString(p.userId.toString),
      "category" -> Json.fromString(p.category),
      "name" -> Json.fromString(p.name),
      "value" -> Json.fromString(p.value)
    )

  given PreferenceDecode: Decoder[Preference] = (c: HCursor) =>
    for {
      userId <- c.downField("user_id").as[UserId]
      category <- c.downField("category").as[String]
      name <- c.downField("name").as[String]
      value <- c.downField("value").as[String]
    } yield Preference(userId, category, name, value)

  given StatusUserEncode: Encoder[StatusUser] = {
    case StatusUser.Online => Json.fromString("online")
    case StatusUser.Offline => Json.fromString("offline")
    case StatusUser.Away => Json.fromString("away")
    case StatusUser.Dnd(_) => Json.fromString("dnd")
  }

  given StatusUserDecode(using time: Option[LocalDateTime]): Decoder[StatusUser] = (c: HCursor) =>
    c.as[String].map(_ -> time).map {
      case ("online", _) => StatusUser.Online
      case ("offline", _) => StatusUser.Offline
      case ("away", _) => StatusUser.Away
      case ("dnd", Some(time)) => StatusUser.Dnd(time)
    }

  given UserStatusEncode(using zone: ZoneId): Encoder[UserStatus] = us => {
    Json.obj(List(
      ("user_id" -> Json.fromString(us.userId.toString)).some,
      ("status" -> Encoder[StatusUser].apply(us.status)).some,
      ("last_activity_at" -> Json.fromLong(us.lastActivityAt.atZone(zone).toEpochSecond)).some,
      us.activeChannel.map(a => "active_channel" -> Json.fromString(a.toString)),
      us.status match {
        case StatusUser.Dnd(time) =>
          ("dnd_end_time" -> Json.fromLong(time.atZone(zone).toEpochSecond)).some
        case _ => None
      }
    ).flatten *)
  }

  given UserStatusDecode(using zone: ZoneId): Decoder[UserStatus] = (c: HCursor) =>
    for {
      userId <- c.downField("user_id").as[UserId]
      manual <- c.downField("manual").as[Boolean]
      lastActivityAt <- c.downField("last_activity_at").as[Long].map(Instant.ofEpochSecond).map(LocalDateTime.ofInstant(_, zone))
      activeChannel <- c.downField("active_channel").as[Option[ChannelId]]
      given Option[LocalDateTime] <- c.downField("dnd_end_time").as[Option[Long]].map(_.map(Instant.ofEpochSecond).map(LocalDateTime.ofInstant(_, zone)))
      status <- c.downField("status").as[StatusUser]
    } yield UserStatus(userId, status, manual, lastActivityAt, activeChannel)

  given CustomStatusDurationEncode: Encoder[CustomStatusDuration] = {
    case CustomStatusDuration.ThirtyMinutes => Json.fromString("thirty_minutes")
    case CustomStatusDuration.OneHour => Json.fromString("one_hour")
    case CustomStatusDuration.FourHours => Json.fromString("four_hours")
    case CustomStatusDuration.Today => Json.fromString("today")
    case CustomStatusDuration.ThisWeek => Json.fromString("this_week")
    case CustomStatusDuration.DateAndTime(_) => Json.fromString("date_and_time")
  }

  given CustomStatusDurationDecode(using time: Option[LocalDateTime]): Decoder[CustomStatusDuration] = (c: HCursor) =>
    c.as[String].map(_ -> time).map {
      case ("thirty_minutes", _) => CustomStatusDuration.ThirtyMinutes
      case ("one_hour", _) => CustomStatusDuration.OneHour
      case ("four_hours", _) => CustomStatusDuration.FourHours
      case ("today", _) => CustomStatusDuration.Today
      case ("this_week", _) => CustomStatusDuration.ThisWeek
      case ("date_and_time", Some(time)) => CustomStatusDuration.DateAndTime(time)
    }

  given CustomStatusEncode(using zone: ZoneId): Encoder[CustomStatus] = cs =>
    Json.obj(
      List(
        ("emoji" -> Json.fromString(cs.emoji)).some,
        ("text" -> Json.fromString(cs.text.getOrElse(""))).some,
        cs.duration.map(d => "duration" -> Encoder[CustomStatusDuration].apply(d)),
        cs.duration match {
          case Some(CustomStatusDuration.DateAndTime(time)) =>
            ("expires_at" -> Json.fromLong(time.atZone(zone).toEpochSecond)).some
          case _ => None
        }
      ).flatten *
    )

  given CustomStatusDecode(using zone: ZoneId): Decoder[CustomStatus] = (c: HCursor) =>
    for {
      emoji <- c.downField("emoji").as[String]
      text <- c.downField("text").as[String].map(s =>
        if (s.isBlank)
          None
        else
          s.some
      )
      given Option[LocalDateTime] <- c.downField("expires_at").as[Option[Long]].map(_.map(Instant.ofEpochSecond).map(LocalDateTime.ofInstant(_, zone)))
      duration <- c.downField("duration").as[Option[CustomStatusDuration]]
    } yield CustomStatus(emoji, text, duration)


  given UpdateUserStatusRequestEncode(using zone: ZoneId): Encoder[UpdateUserStatusRequest] = uusr =>
    Json.obj(
      List(
        ("user_id" -> Json.fromString(uusr.userId.toString)).some,
        ("status" -> Encoder[StatusUser].apply(uusr.status)).some,
        uusr.status match
          case StatusUser.Dnd(time) => ("dnd_end_time" -> Json.fromLong(time.atZone(zone).toEpochSecond)).some
          case _ => None
      ).flatten *)

  given ReactionInsightDecode: Decoder[ReactionInsight] = (c: HCursor) =>
    for {
      emojiName <- c.downField("emoji_name").as[String]
      count <- c.downField("count").as[Long]
    } yield ReactionInsight(emojiName, count)


  given ChannelInsightDecode: Decoder[ChannelInsight] = (c: HCursor) =>
    for {
      id <- c.downField("id").as[ChannelId]
      channelType <- c.downField("type").as[String]
      name: String <- c.downField("name").as[String]
      teamId <- c.downField("team_id").as[TeamId]
      messageCount <- c.downField("message_count").as[Long]
    } yield ChannelInsight(id, channelType, name, teamId, messageCount)

  given ListWrapperDecode[T: Decoder]: Decoder[ListWrapper[T]] = (c: HCursor) =>
    for {
      hasNext <- c.downField("has_next").as[Boolean]
      items <- c.downField("items").as[List[T]]
    } yield ListWrapper(hasNext, items)

  given RoleInfoDecode: Decoder[RoleInfo] = (c: HCursor) =>
    for {
      id <- c.downField("id").as[String]
      name <- c.downField("name").as[String]
      displayName <- c.downField("display_name").as[String]
      description <- c.downField("description").as[String]
      permissions <- c.downField("permissions").as[List[String]]
      schemeManaged <- c.downField("scheme_managed").as[Boolean]
    } yield RoleInfo(id, name, displayName, description, permissions, schemeManaged)
}
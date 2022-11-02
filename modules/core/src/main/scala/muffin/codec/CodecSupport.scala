package muffin.codec

import java.time.*
import scala.collection.StringParsers
import scala.deriving.Mirror

import cats.arrow.FunctionK
import cats.syntax.all.{*, given}

import muffin.api.*
import muffin.http.Body
import muffin.model.*

trait Encode[A] {
  def apply(obj: A): String
}

trait Decode[A] {
  def apply(from: String): Either[Throwable, A]
}

trait JsonRequestRawBuilder[To[_], Build]() { self =>
  def field[T: To](fieldName: String, fieldValue: T): JsonRequestRawBuilder[To, Build]

  def field[T: To](fieldName: String, fieldValue: Option[T]): JsonRequestRawBuilder[To, Build] =
    fieldValue match {
      case Some(value) => field(fieldName, value)
      case None        => self
    }

  def build: Build
}

trait JsonRequestBuilder[T, To[_]]() { self =>
  def field[X: To](fieldName: String, fieldValue: X): JsonRequestBuilder[T, To]

  def field[X: To](fieldName: String, fieldValue: T => X): JsonRequestBuilder[T, To]

  def build[X >: T]: To[X]
}

trait JsonResponseBuilder[From[_], Params <: Tuple] {
  def field[X: From](name: String): JsonResponseBuilder[From, X *: Params]

  def build[X](f: PartialFunction[Params, X]): From[X]
}

trait CodecSupport[To[_], From[_]] extends PrimitivesSupport[To, From] {
  def jsonRaw: JsonRequestRawBuilder[To, Body.RawJson]

  def seal[T](f: T => To[T]): To[T]

  def json[T, X: To](f: T => X): To[T]

  def json[T]: JsonRequestBuilder[T, To]

  def parsing[X: From, T](f: X => T): From[T]

  def parsing: JsonResponseBuilder[From, EmptyTuple]

  given EncodeTo[A: To]: Encode[A]

  given DecodeFrom[A: From]: Decode[A]

  given MapFrom[A: From]: From[Map[String, A]]

  given LoginTo: To[Login] = json(_.toString)

  given UserIdTo: To[UserId] = json(_.toString)

  given GroupIdTo: To[GroupId] = json(_.toString)

  given TeamIdTo: To[TeamId] = json(_.toString)

  given ChannelIdTo: To[ChannelId] = json(_.toString)

  given MessageIdTo: To[MessageId] = json(_.toString)

  given EmojiIdTo: To[EmojiId] = json(_.toString)

  given LoginFrom: From[Login] = parsing[String, Login](Login(_))

  given UserIdFrom: From[UserId] = parsing[String, UserId](UserId(_))

  given GroupIdFrom: From[GroupId] = parsing[String, GroupId](GroupId(_))

  given TeamIdFrom: From[TeamId] = parsing[String, TeamId](TeamId(_))

  given ChannelIdFrom: From[ChannelId] = parsing[String, ChannelId](ChannelId(_))

  given MessageIdFrom: From[MessageId] = parsing[String, MessageId](MessageId(_))

  given EmojiIdFrom: From[EmojiId] = parsing[String, EmojiId](EmojiId(_))

  // Channels
  given NotifyOptionFrom: From[NotifyOption] = parsing[String, NotifyOption](op => NotifyOption.valueOf(op.capitalize))

  given UnreadOptionFrom: From[UnreadOption] = parsing[String, UnreadOption](op => UnreadOption.valueOf(op.capitalize))

  given NotifyPropsFrom: From[NotifyProps] =
    parsing
      .field[UnreadOption]("mark_unread")
      .field[NotifyOption]("desktop")
      .field[NotifyOption]("push")
      .field[NotifyOption]("email")
      .build {
        case t => NotifyProps.apply.tupled(t)
      }

  given ChannelMemberFrom(using zone: ZoneId): From[ChannelMember] =
    parsing
      .field[Option[String]]("team_update_at")
      .field[Option[String]]("team_name")
      .field[Option[String]]("team_display_name")
      .field[Option[LocalDateTime]]("last_viewed_at")
      .field[NotifyProps]("notify_props")
      .field[Option[Long]]("mention_count")
      .field[Option[Long]]("msg_count")
      .field[LocalDateTime]("last_viewed_at")
      .field[String]("roles")
      .field[UserId]("user_id")
      .field[ChannelId]("channel_id")
      .build {
        case t => ChannelMember.apply.tupled(t)
      }

  given ChannelInfoFrom(using zone: ZoneId): From[ChannelInfo] =
    parsing
      .field[UserId]("creator_id")
      .field[Long]("total_msg_count")
      .field[Option[LocalDateTime]]("last_post_at")
      .field[String]("purpose")
      .field[String]("header")
      .field[String]("name")
      .field[String]("display_name")
      .field[String]("type")
      .field[TeamId]("team_id")
      .field[Option[LocalDateTime]]("delete_at")
      .field[LocalDateTime]("update_at")
      .field[LocalDateTime]("create_at")
      .field[ChannelId]("id")
      .build {
        case t => ChannelInfo.apply.tupled(t)
      }
  // Channels

  // Emoji
  given EmojiInfoFrom(using zone: ZoneId): From[EmojiInfo] =
    parsing
      .field[Option[LocalDateTime]]("delete_at")
      .field[LocalDateTime]("update_at")
      .field[LocalDateTime]("create_at")
      .field[String]("name")
      .field[UserId]("creator_id")
      .field[EmojiId]("id")
      .build {
        case t => EmojiInfo.apply.tupled(t)
      }
  // Emoji

  // Preferences
  given PreferenceEncode: To[Preference] =
    json[Preference]
      .field("user_id", _.userId)
      .field("category", _.category)
      .field("name", _.category)
      .field("value", _.value)
      .build

  given PreferenceDecode: From[Preference] =
    parsing
      .field[String]("value")
      .field[String]("name")
      .field[String]("category")
      .field[UserId]("user_id")
      .build {
        case t => Preference.apply.tupled(t)
      }
  // Preferences

  // Status
  given StatusUserEncode: To[StatusUser] =
    json[StatusUser, String] {
      case StatusUser.Online  => "online"
      case StatusUser.Offline => "offline"
      case StatusUser.Away    => "away"
      case StatusUser.Dnd(_)  => "dnd"
    }

  given RawStatusUserDecode: From[RawStatusUser] =
    parsing[String, RawStatusUser] {
      case "online"  => RawStatusUser.Online
      case "offline" => RawStatusUser.Offline
      case "away"    => RawStatusUser.Away
      case "dnd"     => RawStatusUser.Dnd
    }

  given UserStatusEncode(using zone: ZoneId): To[UserStatus] =
    json[UserStatus]
      .field("user_id", _.userId)
      .field("status", _.status)
      .field[Long]("last_activity_at", _.lastActivityAt.atZone(zone).toEpochSecond)
      .field("active_channel", _.activeChannel)
      .field(
        "dnd_end_time",
        _.status match {
          case StatusUser.Dnd(time) => time.atZone(zone).toEpochSecond.some
          case _                    => none[Long]
        }
      )
      .build

  given UserStatusDecode(using zone: ZoneId): From[UserStatus] =
    parsing
      .field[Option[LocalDateTime]]("dnd_end_time")
      .field[Option[ChannelId]]("active_channel")
      .field[LocalDateTime]("last_activity_at")
      .field[Boolean]("manual")
      .field[RawStatusUser]("status")
      .field[UserId]("user_id")
      .build {
        case userId *: status *: manual *: lastActivityAt *: activeChannel *: dndTime *: EmptyTuple =>
          UserStatus(
            userId,
            StatusUser(status, dndTime).getOrElse(StatusUser.Offline),
            manual,
            lastActivityAt,
            activeChannel
          )
      }

  given CustomStatusDurationEncode: To[CustomStatusDuration] =
    json[CustomStatusDuration, String] {
      case CustomStatusDuration.ThirtyMinutes  => "thirty_minutes"
      case CustomStatusDuration.OneHour        => "one_hour"
      case CustomStatusDuration.FourHours      => "four_hours"
      case CustomStatusDuration.Today          => "today"
      case CustomStatusDuration.ThisWeek       => "this_week"
      case CustomStatusDuration.DateAndTime(_) => "date_and_time"
    }

  given CustomStatusDurationDecode: From[RawCustomStatusDuration] =
    parsing[String, RawCustomStatusDuration] {
      case "thirty_minutes" => RawCustomStatusDuration.ThirtyMinutes
      case "one_hour"       => RawCustomStatusDuration.OneHour
      case "four_hours"     => RawCustomStatusDuration.FourHours
      case "today"          => RawCustomStatusDuration.Today
      case "this_week"      => RawCustomStatusDuration.ThisWeek
      case "date_and_time"  => RawCustomStatusDuration.DateAndTime
    }

  given CustomStatusEncode(using zone: ZoneId): To[CustomStatus] =
    json[CustomStatus]
      .field("emoji", _.emoji)
      .field("text", _.text.getOrElse(""))
      .field("duration", _.duration)
      .field(
        "expires_at",
        _.duration match {
          case Some(CustomStatusDuration.DateAndTime(time)) => time.atZone(zone).toEpochSecond.some
          case _                                            => none[Long]
        }
      )
      .build

  given CustomStatusDecode(using zone: ZoneId): From[CustomStatus] =
    parsing
      .field[Option[RawCustomStatusDuration]]("duration")
      .field[Option[LocalDateTime]]("expires_at")
      .field[String]("text")
      .field[String]("emoji")
      .build {
        case emoji *: text *: expiries *: duration *: EmptyTuple =>
          CustomStatus(
            emoji,
            if (text.isBlank)
              None
            else
              text.some,
            duration.flatMap(CustomStatusDuration(_, expiries))
          )
      }

  given UpdateUserStatusRequestEncode(using zone: ZoneId): To[UpdateUserStatusRequest] =
    json[UpdateUserStatusRequest]
      .field("user_id", _.userId)
      .field("status", _.status)
      .field(
        "dnd_end_time",
        _.status match {
          case StatusUser.Dnd(time) => time.atZone(zone).toEpochSecond.some
          case _                    => none[Long]
        }
      )
      .build
  // Status

  // Insights
  given ReactionInsightDecode: From[ReactionInsight] =
    parsing
      .field[Long]("count")
      .field[String]("emoji_name")
      .build {
        case t => ReactionInsight.apply.tupled(t)
      }

  given ChannelInsightDecode: From[ChannelInsight] =
    parsing
      .field[Long]("message_count")
      .field[TeamId]("team_id")
      .field[String]("name")
      .field[String]("type")
      .field[ChannelId]("id")
      .build {
        case t => ChannelInsight.apply.tupled(t)
      }

  given ListWrapperDecode[T: From]: From[ListWrapper[T]] =
    parsing
      .field[List[T]]("items")
      .field[Boolean]("has_next")
      .build {
        case t => ListWrapper[T].apply.tupled(t)
      }
  // Insights

  // Roles
  given RoleInfoDecode: From[RoleInfo] =
    parsing
      .field[Boolean]("scheme_managed")
      .field[List[String]]("permissions")
      .field[String]("description")
      .field[String]("display_name")
      .field[String]("name")
      .field[String]("id")
      .build {
        case t => RoleInfo.apply.tupled(t)
      }
  // Roles

  given DialogTo[T: To]: To[Dialog[T]] =
    json[Dialog[T]]
      .field("callback_id", _.callbackId)
      .field("title", _.title)
      .field("introduction_text", _.introductionText)
      .field("submit_label", _.submitLabel)
      .field("notify_on_cancel", _.notifyOnCancel)
      .field[String]("state", d => summon[Encode[T]].apply(d.state))
      .field("elements", _.elements)
      .build

  given DialogElementTo: To[Element] =
    seal {
      case value: Element.Text     =>
        json
          .field("display_name", value.displayName)
          .field("name", value.name)
          .field("subtype", value.subtype)
          .field("optional", value.optional)
          .field("min_length", value.minLength)
          .field("max_length", value.maxLength)
          .field("help_text", value.helpText)
          .field("default", value.default)
          .field("type", "text")
          .build
      case value: Element.Textarea =>
        json
          .field("display_name", value.displayName)
          .field("name", value.name)
          .field("subtype", value.subtype)
          .field("optional", value.optional)
          .field("min_length", value.minLength)
          .field("max_length", value.maxLength)
          .field("help_text", value.helpText)
          .field("default", value.default)
          .field("type", "textarea")
          .build
      case value: Element.Select   =>
        json
          .field("display_name", value.displayName)
          .field("name", value.name)
          .field("data_source", value.dataSource)
          .field("options", value.options)
          .field("optional", value.optional)
          .field("help_text", value.helpText)
          .field("default", value.default)
          .field("placeholder", value.placeholder)
          .field("type", "select")
          .build
      case value: Element.Checkbox =>
        json
          .field("display_name", value.displayName)
          .field("name", value.name)
          .field("optional", value.optional)
          .field("help_text", value.helpText)
          .field("default", value.default)
          .field("placeholder", value.placeholder)
          .field("type", "bool")
          .build
      case value: Element.Radio    =>
        json
          .field("display_name", value.displayName)
          .field("name", value.name)
          .field("options", value.options)
          .field("help_text", value.helpText)
          .field("default", value.default)
          .field("type", "radio")
          .build
    }

  given DataSourceTo: To[DataSource] =
    json[DataSource, String] {
      case DataSource.Users    => "users"
      case DataSource.Channels => "channels"
    }

  given DataSourceFrom: From[DataSource] =
    parsing[String, DataSource] {
      case "users"    => DataSource.Users
      case "channels" => DataSource.Channels
    }

  given SelectOptionTo: To[SelectOption] = json[SelectOption].field("text", _.text).field("value", _.value).build

  given SelectOptionFrom: From[SelectOption] =
    parsing
      .field[String]("value")
      .field[String]("text")
      .build {
        case t => SelectOption.apply.tupled(t)
      }

  given TextSubtypeTo: To[TextSubtype] =
    json[TextSubtype, String] {
      case TextSubtype.Text     => "text"
      case TextSubtype.Email    => "email"
      case TextSubtype.Number   => "number"
      case TextSubtype.Password => "password"
      case TextSubtype.Tel      => "tel"
      case TextSubtype.Url      => "url"
    }

  given ReactionInfoFrom(using zone: ZoneId): From[ReactionInfo] =
    parsing
      .field[LocalDateTime]("create_at")
      .field[String]("emoji_name")
      .field[MessageId]("post_id")
      .field[UserId]("user_id")
      .build {
        case t => ReactionInfo.apply.tupled(t)
      }

  //  input
  given DialogContextFrom[T: From]: From[DialogAction[T]] =
    parsing
      .field[Boolean]("cancelled")
      .field[Map[String, String]]("submission")
      .field[TeamId]("team_id")
      .field[ChannelId]("channel_id")
      .field[UserId]("user_id")
      .field[T]("state")
      .field[Option[String]]("callback_id")
      .build {
        case t => DialogAction[T].apply.tupled(t)
      }

  given MessageActionFrom[T: From]: From[MessageAction[T]] =
    parsing
      .field[T]("context")
      .field[String]("type")
      .field[String]("data_source")
      .field[String]("trigger_id")
      .field[MessageId]("post_id")
      .field[String]("team_domain")
      .field[TeamId]("team_id")
      .field[String]("channel_name")
      .field[ChannelId]("channel_id")
      .field[Login]("user_name")
      .field[UserId]("user_id")
      .build {
        case t => MessageAction[T].apply.tupled(t)
      }

  given AppResponseTo[T: To]: To[AppResponse[T]] =
    seal {
      case AppResponse.Ok()                                     => json.build
      case AppResponse.Message(text, responseType, attachments) =>
        json
          .field("text", text)
          .field("response_type", responseType)
          .field[List[Attachment[T]]]("attachments", attachments)
          .build
      case AppResponse.Errors(map)                              => json.field("errors", map).build
    }

  given ResponseTypeTo: To[ResponseType] =
    json[ResponseType, String] {
      case ResponseType.Ephemeral => "ephemeral"
      case ResponseType.InChannel => "in_channel"
    }

  //  input
  given UserFrom(using zone: ZoneId): From[User] =
    parsing
      .field[Option[String]]("locale")
      .field[Option[String]]("roles")
      .field[Option[String]]("email")
      .field[Option[String]]("nickname")
      .field[Option[String]]("last_name")
      .field[Option[String]]("first_name")
      .field[String]("username")
      .field[UserId]("id")
      .build {
        case t => User.apply.tupled(t)
      }

  given ActionTo[T: To]: To[Action[T]] =
    seal {
      case Action.Button(id, name, integration, style)               =>
        json
          .field("id", id)
          .field("name", name)
          .field[Integration[T]]("integration", integration)
          .field("style", style)
          .field("type", "button")
          .build
      case Action.Select(id, name, integration, options, dataSource) =>
        json
          .field("id", id)
          .field("name", name)
          .field[Integration[T]]("integration", integration)
          .field("options", options)
          .field("data_source", dataSource)
          .field("type", "select")
          .build
    }

  given ActionFrom[T: From]: From[Action[T]] =
    parsing
      .field[Integration[T]]("integration")
      .field[String]("name")
      .field[String]("id")
      .field[Option[Style]]("style")
      .field[Option[DataSource]]("data_source")
      .field[Option[List[SelectOption]]]("options")
      .field[String]("type")
      .build {
        case "select" *: Some(options) *: dataSource *: None *: id *: name *: integration *: EmptyTuple =>
          Action.Select(id, name, integration, options, dataSource)
        case "button" *: _ *: _ *: Some(style) *: id *: name *: integration *: EmptyTuple               =>
          Action.Button(id, name, integration, style)
      }

  given IntegrationTo[T: To]: To[Integration[T]] =
    seal[Integration[T]] {
      case Integration.Url(url)          => json[Integration.Url].field("url", url).build
      case Integration.Context(url, ctx) =>
        json[Integration.Context[T]].field("url", url).field[T]("context", ctx).build
    }

  given IntegrationFrom[T: From]: From[Integration[T]] =
    parsing
      .field[Option[T]]("integration")
      .field[String]("url")
      .build {
        case url *: Some(integration) *: EmptyTuple => Integration.Context(url, integration)
        case url *: _ *: EmptyTuple                 => Integration.Url(url)
      }

  given StyleTo: To[Style] = json[Style, String](_.toString.toLowerCase)

  given StyleFrom: From[Style] = parsing[String, Style](s => Style.valueOf(s.capitalize))

  given PropsTo[T: To]: To[Props[T]] = json[Props[T]].field[List[Attachment[T]]]("attachments", _.attachments).build

  given AttachmentTo[T: To]: To[Attachment[T]] =
    json[Attachment[T]]
      .field("fallback", _.fallback)
      .field("color", _.color)
      .field("pretext", _.pretext)
      .field("text", _.text)
      .field("author_name", _.authorName)
      .field("author_link", _.authorLink)
      .field("author_icon", _.authorIcon)
      .field("title", _.title)
      .field("title_link", _.titleLink)
      .field[List[AttachmentField]]("fields", _.fields)
      .field("image_url", _.imageUrl)
      .field("thumb_url", _.thumbUrl)
      .field("footer", _.footer)
      .field("footer_icon", _.footerIcon)
      .field[List[Action[T]]]("actions", _.actions)
      .build

  given AttachmentFrom[T: From]: From[Attachment[T]] =
    parsing
      .field[List[Action[T]]]("actions")
      .field[Option[String]]("footer_icon")
      .field[Option[String]]("footer")
      .field[Option[String]]("thumb_url")
      .field[Option[String]]("image_url")
      .field[List[AttachmentField]]("fields")
      .field[Option[String]]("title_link")
      .field[Option[String]]("title")
      .field[Option[String]]("author_icon")
      .field[Option[String]]("author_link")
      .field[Option[String]]("author_name")
      .field[Option[String]]("text")
      .field[Option[String]]("pretext")
      .field[Option[String]]("color")
      .field[Option[String]]("fallback")
      .build {
        case t => Attachment[T].apply.tupled(t)
      }

  given AttachmentFieldTo: To[AttachmentField] =
    json[AttachmentField].field("title", _.title).field("value", _.value).field("short", _.short).build

  given AttachmentFieldFrom: From[AttachmentField] =
    parsing
      .field[Boolean]("short")
      .field[String]("value")
      .field[String]("title")
      .build {
        case t => AttachmentField.apply.tupled(t)
      }

  given PostFrom[T: From]: From[Post[T]] =
    parsing
      .field[MessageId]("id")
      .build {
        case t *: EmptyTuple => Post[T].apply(t)
      }

}

trait PrimitivesSupport[To[_], From[_]] {
  given StringTo: To[String]

  given StringFrom: From[String]

  given OptionTo[A: To]: To[Option[A]]

  given OptionFrom[A: From]: From[Option[A]]

  given LocalDateTimeTo(using zone: ZoneId): To[LocalDateTime]

  given LocalDateTimeFrom(using zone: ZoneId): From[LocalDateTime]

  given BoolTo: To[Boolean]

  given BoolFrom: From[Boolean]

  given LongTo: To[Long]

  given LongFrom: From[Long]

  given ListTo[A: To]: To[List[A]]

  given ListFrom[A: From]: From[List[A]]

  given UnitTo: To[Unit]

  given UnitFrom: From[Unit]

  given NothingTo: To[Nothing]

  given NothingFrom: From[Nothing]

  given AnyTo: To[Any]

  given AnyFrom: From[Any]
}

package muffin.codec

import java.time.*

import cats.Show
import cats.syntax.all.*

import muffin.error.MuffinError
import muffin.http.*
import muffin.internal.*
import muffin.model.*

trait Encode[A] {
  def apply(obj: A): String
}

object Encode {
  def apply[A](using encode: Encode[A]): Encode[A] = encode
}

trait Decode[A] {
  def apply(from: String): Either[MuffinError.Decoding, A]
}

object Decode {
  def apply[A](using decode: Decode[A]): Decode[A] = decode
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
  def rawField(fieldName: String, fieldValue: T => String): JsonRequestBuilder[T, To]

  def field[X: To](fieldName: String, fieldValue: T => X): JsonRequestBuilder[T, To]

  def build: To[T]
}

trait JsonResponseBuilder[From[_], Params <: Tuple] {
  def field[X: From](name: String): JsonResponseBuilder[From, X *: Params]

  def rawField(name: String): JsonResponseBuilder[From, Option[String] *: Params]

  def internal[X: From](name: String): JsonResponseBuilder[From, X *: Params]

  def select[X](f: PartialFunction[Params, From[X]]): From[X]

  def build[X](f: PartialFunction[Params, X]): From[X]

  def build[X](f: Params => X): From[X] = build(PartialFunction.fromFunction(f))
}

trait CodecSupport[To[_], From[_]] extends CodecSupportLow[To, From] {
  given NothingTo: To[Nothing]

  given NothingFrom: From[Nothing]
}

trait CodecSupportLow[To[_], From[_]] extends PrimitivesSupport[To, From] {
  given EncodeTo[A: To]: Encode[A]

  given DecodeFrom[A: From]: Decode[A]

  given MapFrom[A: From]: From[Map[String, A]]

  // Channels
  given NotifyOptionFrom: From[NotifyOption] = parsing[String, NotifyOption](op => NotifyOption.valueOf(op.capitalize))

  given UnreadOptionFrom: From[UnreadOption] = parsing[String, UnreadOption](op => UnreadOption.valueOf(op.capitalize))

  given NotifyPropsFrom: From[NotifyProps] =
    parsing
      .field[UnreadOption]("mark_unread")
      .field[NotifyOption]("desktop")
      .field[NotifyOption]("push")
      .field[NotifyOption]("email")
      .build(NotifyProps.apply.tupled)

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
      .build(ChannelMember.apply.tupled)

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
      .build(ChannelInfo.apply.tupled)
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
      .build(EmojiInfo.apply.tupled)
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
      .build(Preference.apply.tupled)
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
      .build(ReactionInsight.apply.tupled)

  given ChannelInsightDecode: From[ChannelInsight] =
    parsing
      .field[Long]("message_count")
      .field[TeamId]("team_id")
      .field[String]("name")
      .field[String]("type")
      .field[ChannelId]("id")
      .build(ChannelInsight.apply.tupled)

  given ListWrapperDecode[T: From]: From[ListWrapper[T]] =
    parsing
      .field[List[T]]("items")
      .field[Boolean]("has_next")
      .build(ListWrapper[T].apply.tupled)
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
      .build(RoleInfo.apply.tupled)
  // Roles

  given DialogTo: To[Dialog] =
    json[Dialog]
      .field("callback_id", _.callbackId)
      .field("title", _.title)
      .field("introduction_text", _.introductionText)
      .field("submit_label", _.submitLabel)
      .field("notify_on_cancel", _.notifyOnCancel)
      .rawField("state", _.state)
      .field("elements", _.elements)
      .build

  given DialogElementTo: To[Element] =
    seal[Element] {
      case value: Element.Text     =>
        json[Element]
          .field("display_name", _ => value.displayName)
          .field("name", _ => value.name)
          .field("subtype", _ => value.subtype)
          .field("optional", _ => value.optional)
          .field("min_length", _ => value.minLength)
          .field("max_length", _ => value.maxLength)
          .field("help_text", _ => value.helpText)
          .field("default", _ => value.default)
          .field("type", _ => "text")
          .build
      case value: Element.Textarea =>
        json[Element]
          .field("display_name", _ => value.displayName)
          .field("name", _ => value.name)
          .field("subtype", _ => value.subtype)
          .field("optional", _ => value.optional)
          .field("min_length", _ => value.minLength)
          .field("max_length", _ => value.maxLength)
          .field("help_text", _ => value.helpText)
          .field("default", _ => value.default)
          .field("type", _ => "textarea")
          .build
      case value: Element.Select   =>
        json[Element]
          .field("display_name", _ => value.displayName)
          .field("name", _ => value.name)
          .field("data_source", _ => value.dataSource)
          .field("options", _ => value.options)
          .field("optional", _ => value.optional)
          .field("help_text", _ => value.helpText)
          .field("default", _ => value.default)
          .field("placeholder", _ => value.placeholder)
          .field("type", _ => "select")
          .build
      case value: Element.Checkbox =>
        json[Element]
          .field("display_name", _ => value.displayName)
          .field("name", _ => value.name)
          .field("optional", _ => value.optional)
          .field("help_text", _ => value.helpText)
          .field("default", _ => value.default)
          .field("placeholder", _ => value.placeholder)
          .field("type", _ => "bool")
          .build
      case value: Element.Radio    =>
        json[Element]
          .field("display_name", _ => value.displayName)
          .field("name", _ => value.name)
          .field("options", _ => value.options)
          .field("help_text", _ => value.helpText)
          .field("default", _ => value.default)
          .field("type", _ => "radio")
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
      .build(SelectOption.apply.tupled)

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
      .build(ReactionInfo.apply.tupled)

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
      .build(DialogAction[T].apply.tupled)

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
      .build(MessageAction[T].apply.tupled)

  given AppResponseTo: To[AppResponse] =
    seal {
      case AppResponse.Ok()                                     =>
        json[AppResponse]
          .build
      case AppResponse.Message(text, responseType, attachments) =>
        json[AppResponse]
          .field("text", _ => text)
          .field("response_type", _ => responseType)
          .field[List[Attachment]]("attachments", _ => attachments)
          .build
      case AppResponse.Errors(map)                              =>
        json[AppResponse]
          .field("errors", _ => map)
          .build
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
      .build(User.apply.tupled)

  given ActionTo: To[Action] =
    seal[Action] {
      case button: Action.Button =>
        json[Action]
          .field("id", _ => button.id)
          .field("name", _ => button.name)
          .field[RawIntegration]("integration", _ => button.raw)
          .field("style", _ => button.style)
          .field("type", _ => "button")
          .build
      case select: Action.Select =>
        json[Action]
          .field("id", _ => select.id)
          .field("name", _ => select.name)
          .field[RawIntegration]("integration", _ => select.raw)
          .field("options", _ => select.options)
          .field("data_source", _ => select.dataSource)
          .field("type", _ => "select")
          .build
    }

  given ActionFrom: From[Action] =
    parsing
      .field[RawIntegration]("integration")
      .field[String]("name")
      .field[String]("id")
      .field[String]("type")
      .select {
        case "select" *: id *: name *: integration *: EmptyTuple =>
          parsing
            .field[Option[DataSource]]("data_source")
            .field[Option[List[SelectOption]]]("options")
            .build {
              case options *: dataSource *: EmptyTuple =>
                Action.Select(id, name, options.toList.flatten, dataSource)(integration)
            }
        case "button" *: id *: name *: integration *: EmptyTuple =>
          parsing
            .field[Option[Style]]("style")
            .build {
              case style *: EmptyTuple => Action.Button(id, name, style.getOrElse(Style.Default))(integration)
            }
      }

  given IntegrationTo: To[RawIntegration] =
    seal[RawIntegration] {
      case RawIntegration(url, Some(ctx)) =>
        json[RawIntegration]
          .field("url", _ => url)
          .rawField("context", _ => ctx)
          .build
      case RawIntegration(url, _)         =>
        json[RawIntegration]
          .field("url", _ => url)
          .build
    }

  given IntegrationFrom: From[RawIntegration] =
    parsing
      .rawField("context")
      .field[String]("url")
      .build {
        case url *: integration *: EmptyTuple => RawIntegration(url, integration)
      }

  given StyleTo: To[Style] = json[Style, String](_.toString.toLowerCase)

  given StyleFrom: From[Style] = parsing[String, Style](s => Style.valueOf(s.capitalize))

  given PropsTo: To[Props] = json[Props].field[List[Attachment]]("attachments", _.attachments).build

  given PropsFrom: From[Props] =
    parsing
      .field[Option[List[Attachment]]]("attachments")
      .build {
        case attachments *: EmptyTuple => Props(attachments.getOrElse(List.empty))
      }

  given AttachmentTo: To[Attachment] =
    json[Attachment]
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
      .field[List[Action]]("actions", _.actions)
      .build

  given AttachmentFrom: From[Attachment] =
    parsing
      .field[List[Action]]("actions")
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
      .build(Attachment.apply.tupled)

  given AttachmentFieldTo: To[AttachmentField] =
    json[AttachmentField].field("title", _.title)
      .field("value", _.value)
      .field("short", _.short)
      .build

  given AttachmentFieldFrom: From[AttachmentField] =
    parsing
      .field[Boolean]("short")
      .field[String]("value")
      .field[String]("title")
      .build(AttachmentField.apply.tupled)

  given PostFrom: From[Post] =
    parsing
      .field[Option[Props]]("props")
      .field[String]("message")
      .field[MessageId]("id")
      .build {
        case id *: message *: props *: EmptyTuple => Post(id, message, props.getOrElse(Props.empty))
      }

}

trait PrimitivesSupport[To[_], From[_]] extends NewTypeSupport[To, From] {
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

  given IntTo: To[Int]

  given IntFrom: From[Int]

  given ListTo[A: To]: To[List[A]]

  given ListFrom[A: From]: From[List[A]]

  given UnitTo: To[Unit]

  given UnitFrom: From[Unit]

  given AnyTo: To[Any]

  given AnyFrom: From[Any]

  given MapTo[K: To, V: To]: To[Map[K, V]]
}

trait NewTypeSupport[To[_], From[_]] extends CodecSyntax[To, From] {
  given NewTypeTo[A, B](using cc: Coercible[To[A], To[B]], to: To[A]): To[B] = cc(to)

  given NewTypeFrom[A, B](using cc: Coercible[From[A], From[B]], from: From[A]): From[B] = cc(from)

  given NewTypeShow[A, B](using cc: Coercible[Show[A], Show[B]], show: Show[A]): Show[B] = cc(show)
}

trait CodecSyntax[To[_], From[_]] {
  def jsonRaw: JsonRequestRawBuilder[To, Body.RawJson]

  def seal[T](f: T => To[T]): To[T]

  def json[T, X: To](f: T => X): To[T]

  def json[T]: JsonRequestBuilder[T, To]

  def parsing[X: From, T](f: X => T): From[T]

  def parsing: JsonResponseBuilder[From, EmptyTuple]
}

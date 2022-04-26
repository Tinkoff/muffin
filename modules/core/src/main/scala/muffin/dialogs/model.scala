package muffin.dialogs

import io.circe.{Encoder, Json, JsonObject}
import io.circe.syntax.given

case class OpenDialogRequest(
  trigger_id: String, // TODO make Id
  url: String, // TODO make URL
  dialog: Dialog
) derives Encoder.AsObject

case class Dialog(
  callback_id: String,
  title: String,
  introduction_text: String,
  elements: List[DialogElement],
  submit_label: Option[String] = None,
  notify_on_cancel: Boolean = false,
  state: String
) derives Encoder.AsObject

sealed trait DialogElement

object DialogElement {
  given encoder: Encoder[DialogElement] = {
    case value: Text     => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("text"))).dropNullValues
    case value: Textarea => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("textarea"))).dropNullValues
    case value: Select   => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("select"))).dropNullValues
    case value: Checkbox => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("bool"))).dropNullValues
    case value: Radio    => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("radio"))).dropNullValues
  }
}

enum TextSubtype:
  case Text
  case Email
  case Number
  case Password
  case Tel
  case Url

object TextSubtype {
  given encoder: Encoder[TextSubtype] = {
    case Text     => Encoder.encodeString("text")
    case Email    => Encoder.encodeString("email")
    case Number   => Encoder.encodeString("number")
    case Password => Encoder.encodeString("password")
    case Tel      => Encoder.encodeString("tel")
    case Url      => Encoder.encodeString("url")
  }
}

enum DataSource:
  case Users
  case Channels

case class Text(
  display_name: String,
  name: String,
  subtype: TextSubtype = TextSubtype.Text,
  optional: Boolean = false,
  min_length: Option[Int] = None,
  max_length: Option[Int] = None,
  help_text: Option[String] = None,
  default: Option[String] = None
) extends DialogElement
    derives Encoder.AsObject

case class Textarea(
  display_name: String,
  name: String,
  subtype: TextSubtype = TextSubtype.Text,
  optional: Boolean = false,
  min_length: Option[Int] = None,
  max_length: Option[Int] = None,
  help_text: Option[String] = None,
  default: Option[String] = None
) extends DialogElement
    derives Encoder.AsObject

case class SelectOption(text: String, value: String) derives Encoder.AsObject

case class Select(
  display_name: String,
  name: String,
  data_source: Option[DataSource] = None,
  options: List[SelectOption],
  optional: Boolean = false,
  help_text: Option[String] = None,
  default: Option[String] = None,
  placeholder: Option[String] = None
) extends DialogElement
    derives Encoder.AsObject

case class Checkbox(
  display_name: String,
  name: String,
  optional: Boolean = false,
  help_text: Option[String] = None,
  default: Option[Boolean] = None,
  placeholder: Option[String] = None
) extends DialogElement
    derives Encoder.AsObject

case class Radio(
  display_name: String,
  name: String,
  options: List[SelectOption],
  help_text: Option[String] = None,
  default: Option[Boolean] = None
) extends DialogElement
    derives Encoder.AsObject

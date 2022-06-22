package muffin.api.dialogs

import io.circe.{Encoder, Json, JsonObject}
import io.circe.syntax.given

case class Dialog(
  callbackId: String,
  title: String,
  introductionText: String,
  elements: List[DialogElement],
  submitLabel: Option[String] = None,
  notifyOnCancel: Boolean = false,
  state: String
)

sealed trait DialogElement

//object DialogElement {
//  given encoder: Encoder[DialogElement] = {
//    case value: Text     => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("text"))).dropNullValues
//    case value: Textarea => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("textarea"))).dropNullValues
//    case value: Select   => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("select"))).dropNullValues
//    case value: Checkbox => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("bool"))).dropNullValues
//    case value: Radio    => Json.fromJsonObject(value.asJsonObject.add("type", Json.fromString("radio"))).dropNullValues
//  }
//}

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
  displayName: String,
  name: String,
  subtype: TextSubtype = TextSubtype.Text,
  optional: Boolean = false,
  minLength: Option[Int] = None,
  maxLength: Option[Int] = None,
  helpText: Option[String] = None,
  default: Option[String] = None
) extends DialogElement

case class Textarea(
  displayName: String,
  name: String,
  subtype: TextSubtype = TextSubtype.Text,
  optional: Boolean = false,
  minLength: Option[Int] = None,
  maxLength: Option[Int] = None,
  helpText: Option[String] = None,
  default: Option[String] = None
) extends DialogElement

case class SelectOption(text: String, value: String)

case class Select(
  displayName: String,
  name: String,
  dataSource: Option[DataSource] = None,
  options: List[SelectOption],
  optional: Boolean = false,
  helpText: Option[String] = None,
  default: Option[String] = None,
  placeholder: Option[String] = None
) extends DialogElement

case class Checkbox(
  displayName: String,
  name: String,
  optional: Boolean = false,
  helpText: Option[String] = None,
  default: Option[Boolean] = None,
  placeholder: Option[String] = None
) extends DialogElement

case class Radio(
  displayName: String,
  name: String,
  options: List[SelectOption],
  helpText: Option[String] = None,
  default: Option[Boolean] = None
) extends DialogElement

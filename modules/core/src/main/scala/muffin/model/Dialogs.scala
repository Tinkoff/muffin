package muffin.model

case class Dialog[T](
    title: String,
    state: T,
    callbackId: Option[String] = None,
    introductionText: Option[String] = None,
    elements: List[Element] = Nil,
    submitLabel: Option[String] = None,
    notifyOnCancel: Boolean = false
)

enum TextSubtype {
  case Text
  case Email
  case Number
  case Password
  case Tel
  case Url
}

sealed trait Element

object Element {

  case class Text(
      displayName: String,
      name: String,
      subtype: TextSubtype = TextSubtype.Text,
      optional: Boolean = false,
      minLength: Option[Long] = None,
      maxLength: Option[Long] = None,
      helpText: Option[String] = None,
      default: Option[String] = None
  ) extends Element

  case class Textarea(
      displayName: String,
      name: String,
      subtype: TextSubtype = TextSubtype.Text,
      optional: Boolean = false,
      minLength: Option[Long] = None,
      maxLength: Option[Long] = None,
      helpText: Option[String] = None,
      default: Option[String] = None
  ) extends Element

  case class Select(
      displayName: String,
      name: String,
      dataSource: Option[DataSource] = None,
      options: List[SelectOption],
      optional: Boolean = false,
      helpText: Option[String] = None,
      default: Option[String] = None,
      placeholder: Option[String] = None
  ) extends Element

  case class Checkbox(
      displayName: String,
      name: String,
      optional: Boolean = false,
      helpText: Option[String] = None,
      default: Option[String] = None,
      placeholder: Option[String] = None
  ) extends Element

  case class Radio(
      displayName: String,
      name: String,
      options: List[SelectOption],
      helpText: Option[String] = None,
      default: Option[String] = None
  ) extends Element

}

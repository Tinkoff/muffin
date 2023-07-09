package muffin.model

import muffin.codec.{Decode, Encode}

case class Dialog private[muffin] (
    title: String,
    callbackId: Option[String] = None,
    introductionText: Option[String] = None,
    elements: List[Element] = Nil,
    submitLabel: Option[String] = None,
    notifyOnCancel: Boolean = false
)(private[muffin] val state: String) {

  def copyState[T: Encode](state: T) =
    Dialog(title, callbackId, introductionText, elements, submitLabel, notifyOnCancel)(Encode[T].apply(state))

  def state[T: Decode]: Option[T] = Decode[T].apply(state).toOption
}

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
      minLength: Option[Int] = None,
      maxLength: Option[Int] = None,
      helpText: Option[String] = None,
      default: Option[String] = None
  ) extends Element

  case class Textarea(
      displayName: String,
      name: String,
      subtype: TextSubtype = TextSubtype.Text,
      optional: Boolean = false,
      minLength: Option[Int] = None,
      maxLength: Option[Int] = None,
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

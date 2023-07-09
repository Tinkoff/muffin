package muffin.model

import muffin.codec.{Decode, Encode}

class Dialog private[muffin] (
    val title: String,
    val callbackId: Option[String] = None,
    val introductionText: Option[String] = None,
    val elements: List[Element] = Nil,
    val submitLabel: Option[String] = None,
    val notifyOnCancel: Boolean = false
)(private[muffin] val state: String) {

  def copy(
      title: String = this.title,
      callbackId: Option[String] = this.callbackId,
      introductionText: Option[String] = this.callbackId,
      elements: List[Element] = this.elements,
      submitLabel: Option[String] = this.submitLabel,
      notifyOnCancel: Boolean = this.notifyOnCancel
  ): Dialog = new Dialog(title, callbackId, introductionText, elements, submitLabel, notifyOnCancel)(state)

  def copyState[T: Encode](state: T) =
    new Dialog(title, callbackId, introductionText, elements, submitLabel, notifyOnCancel)(Encode[T].apply(state))

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

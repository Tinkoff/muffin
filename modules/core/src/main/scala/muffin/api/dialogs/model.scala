package muffin.api.dialogs

import muffin.api.posts.{SelectOption, DataSource}

case class Dialog(
                   callbackId: String,
                   title: String,
                   introductionText: String,
                   elements: List[Element],
                   submitLabel: Option[String] = None,
                   notifyOnCancel: Boolean = false,
                   state: String
                 )



enum TextSubtype:
  case Text
  case Email
  case Number
  case Password
  case Tel
  case Url

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
                       default: Option[Boolean] = None,
                       placeholder: Option[String] = None
                     ) extends Element

  case class Radio(
                    displayName: String,
                    name: String,
                    options: List[SelectOption],
                    helpText: Option[String] = None,
                    default: Option[Boolean] = None
                  ) extends Element
}


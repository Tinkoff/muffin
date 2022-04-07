package muffin.dialogs

import io.circe.Encoder

case class OpenDialogRequest(
  triggerId: String, // TODO make Id
  url: String, // TODO make URL
  dialog: Dialog
) derives Encoder.AsObject

case class Dialog(
  callbackId: String,
  title: String,
  introductionText: String,
  //                   elements:List[]

  submitLabel: Option[String] = None,
  notifyOnCancel: Boolean = false,
  state: String
) derives Encoder.AsObject

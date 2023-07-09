package muffin.api

import cats.syntax.all.given

import org.scalatest.Tag

import muffin.api.ApiTestSupport
import muffin.codec.{CodecSupport, Decode, Encode}
import muffin.dsl.*
import muffin.model.*

trait DslTest[To[_], From[_]](integration: String, codecSupport: CodecSupport[To, From]) extends ApiTestSupport {

  import codecSupport.{*, given}

  given Encoder: Encode[State]
  given Decoder: Decode[State]

  case class State(value1: String, value2: Int)

  Feature("Dialog") {
    Scenario(s"create dialog in $integration", Tag(integration)) {
      val state = State("value", 123)

      val dialogDsl: Dialog =
        dialog("Dialog", state)
          .callback("Callback")
          .submitLabel("Submit")
          .introduction("introduction")
          .notifyOnCancel
          .element(Element.Text(
            displayName = "display text",
            name = "name text",
            subtype = TextSubtype.Email,
            optional = true,
            minLength = 100.some,
            maxLength = 150.some,
            helpText = "help text".some,
            default = "default text".some
          ))
          .element(Element.Textarea(
            displayName = "display textarea",
            name = "name textarea",
            subtype = TextSubtype.Number,
            optional = true,
            minLength = 10.some,
            maxLength = 20.some,
            helpText = "help textarea".some,
            default = "default textarea".some
          ))
          .element(Element.Radio(
            displayName = "display textarea",
            name = "name textarea",
            options = List(
              SelectOption("option 1", "value 1"),
              SelectOption("option 2", "value 2")
            ),
            helpText = "help textarea".some,
            default = "default textarea".some
          ))
          .element(Element.Select(
            displayName = "display textarea",
            name = "name textarea",
            options = List(
              SelectOption("option 1", "value 1"),
              SelectOption("option 2", "value 2")
            ),
            helpText = "help textarea".some,
            default = "default textarea".some
          ))
          .notifyOnCancel
          .make

      assert(dialogDsl.state[State].contains(state))

      val dialogRaw =
        new Dialog(
          title = "Dialog",
          callbackId = "Callback".some,
          introductionText = "introduction".some,
          elements = List(
            Element.Text(
              displayName = "display text",
              name = "name text",
              subtype = TextSubtype.Email,
              optional = true,
              minLength = 100.some,
              maxLength = 150.some,
              helpText = "help text".some,
              default = "default text".some
            ),
            Element.Textarea(
              displayName = "display textarea",
              name = "name textarea",
              subtype = TextSubtype.Number,
              optional = true,
              minLength = 10.some,
              maxLength = 20.some,
              helpText = "help textarea".some,
              default = "default textarea".some
            ),
            Element.Radio(
              displayName = "display textarea",
              name = "name textarea",
              options = List(
                SelectOption("option 1", "value 1"),
                SelectOption("option 2", "value 2")
              ),
              helpText = "help textarea".some,
              default = "default textarea".some
            ),
            Element.Select(
              displayName = "display textarea",
              name = "name textarea",
              options = List(
                SelectOption("option 1", "value 1"),
                SelectOption("option 2", "value 2")
              ),
              helpText = "help textarea".some,
              default = "default textarea".some
            )
          ),
          submitLabel = "Submit".some,
          notifyOnCancel = true
        )("""{"value1":"value","value2":123}""")

      assert(dialogDsl == dialogRaw)
    }
  }

  Feature("Message") {
    Scenario(s"create dialog in $integration", Tag(integration)) {
      val state = State("value", 123)

      val attachDsl =
        attachment
          .action(button("button", _.Context("url", state)))
          .action(selectSource("select source", _.Url("url2"), _.Users))
          .make

      val attacRaw = Attachment(actions =
        List(
          Action.Button(
            "button",
            "button"
          )(RawIntegration("url", """{"value1":"value","value2":123}""".some)),
          Action.Select("select source", "select source", Nil, DataSource.Users.some)(RawIntegration("url2", None))
        )
      )

      assert(attachDsl == attacRaw)
    }
  }

}

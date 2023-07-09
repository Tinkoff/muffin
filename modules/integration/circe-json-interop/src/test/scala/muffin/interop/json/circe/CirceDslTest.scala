package muffin.interop.json.circe

import io.circe.{Decoder, Encoder}

import muffin.api.DslTest
import muffin.codec.*

class CirceDslTest extends DslTest[Encoder, Decoder]("circe", codec) {

  import codec.{*, given}

  given Encoder: Encode[State] = EncodeTo(io.circe.Derivation.summonEncoder[State])
  given Decoder: Decode[State] = DecodeFrom(io.circe.Derivation.summonDecoder[State])

}

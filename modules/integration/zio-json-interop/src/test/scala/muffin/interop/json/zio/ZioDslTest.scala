package muffin.interop.json.zio

import zio.json.{JsonDecoder, JsonEncoder}

import muffin.api.DslTest
import muffin.codec.*

class ZioDslTest extends DslTest[JsonEncoder, JsonDecoder]("zio", codec) {

  import codec.{*, given}

  given Encoder: Encode[State] = EncodeTo(JsonEncoder.derived[State])
  given Decoder: Decode[State] = DecodeFrom(JsonDecoder.derived[State])
}

package zio

import zio.json.internal.{RecordingReader, RetractReader}

object MuffinRetract {
  type Reader = RetractReader
  def reader(in: RetractReader): RecordingReader = zio.json.internal.RecordingReader(in)
}

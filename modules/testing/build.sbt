import Dependencies._

libraryDependencies ++= zioCats ::
  sttp.zio ::
  tapir.core ::
  tapir.http4s ::
  tapir.circe ::
  http4s.ember ::
  Nil

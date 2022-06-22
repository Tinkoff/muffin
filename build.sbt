ThisBuild / version := "0.1.23-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.0-RC1-bin-20220530-d6e3d12-NIGHTLY"

ThisBuild / organization := "space.littleinferno"

ThisBuild / scalaOutputVersion := "3.1.2"

lazy val root = (project in file("."))
  .settings(name := "muffin")
  .dependsOn(core)
  .aggregate(core, `sttp-http-interop`, `circe-interop`, `zio-json-interop`, `zio-http-interop`, `http4s-http-interop`)


ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := (s"Tinkoff Releases" at s"https://nexus.tcsbank.ru/repository/api-snapshot").some

//ThisBuild / publishConfiguration := (ThisBuild / publishConfiguration).value.withOverwrite(true)

lazy val modules = file("modules")

lazy val core = project
  .in(modules / "core")

lazy val integration = modules / "integration"

lazy val `sttp-http-interop` = project
  .in(integration / "sttp-http-interop")
  .dependsOn(core)

lazy val `http4s-http-interop` = project
  .in(integration / "http4s-http-interop")
  .dependsOn(core)

lazy val `zio-http-interop` = project
  .in(integration / "zio-http-interop")
  .dependsOn(core)

lazy val `circe-interop` = project
  .in(integration / "circe-interop")
  .dependsOn(core)

lazy val `zio-json-interop` = project
  .in(integration / "zio-json-interop")
  .dependsOn(core)

lazy val testing = project
  .in(modules / "testing")
  .aggregate(core, `sttp-http-interop`, `circe-interop` )
  .dependsOn(core, `sttp-http-interop`, `circe-interop`)
  .settings(publish / skip := true)

ThisBuild / scalacOptions += "-source:future"
ThisBuild / scalacOptions += "-explain"

ThisBuild / credentials ++= Option(Path.userHome / ".ivy2" / ".credentials")
  .filter(_.exists)
  .map(Credentials.apply)

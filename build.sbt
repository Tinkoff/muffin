ThisBuild / version := "0.1.23-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.0-RC1-bin-20220530-d6e3d12-NIGHTLY"

ThisBuild / organization := "space.littleinferno"

ThisBuild / scalaOutputVersion := "3.1.2"

lazy val root = (project in file("."))
  .settings(name := "muffin")
  .dependsOn(core, client, http, app)
  .aggregate(core, client, http, `http-sttp`, app, `circe-interop`, `zio-json-interop`)



ThisBuild / publishMavenStyle := true

//ThisBuild / publishConfiguration := (ThisBuild / publishConfiguration).value.withOverwrite(true)


lazy val modules = file("modules")

lazy val http = project
  .in(modules / "http")
  .dependsOn(core)

lazy val core = project
  .in(modules / "core")

lazy val client = project
  .in(modules / "client")
  .dependsOn(core, http)

lazy val app = project
  .in(modules / "app")
  .dependsOn(core, client, http)

lazy val integration = modules / "integration"

lazy val `http-sttp` = project
  .in(integration / "http-sttp")
  .dependsOn(http)

lazy val `circe-interop` = project
  .in(integration / "circe-interop")
  .dependsOn(core)

lazy val `zio-json-interop` = project
  .in(integration / "zio-json-interop")
  .dependsOn(core)

lazy val testing = project
  .in(modules / "testing")
  .aggregate(core, client, http, `http-sttp`, app, `circe-interop`, `zio-json-interop`)
  .dependsOn(core, client, http, `http-sttp`, app, `circe-interop`, `zio-json-interop`)
  .settings(publish / skip := true)

ThisBuild / scalacOptions += "-source:future"
ThisBuild / scalacOptions += "-explain"

ThisBuild / credentials ++= Option(Path.userHome / ".ivy2" / ".credentials")
  .filter(_.exists)
  .map(Credentials.apply)

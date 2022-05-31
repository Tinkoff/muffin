ThisBuild / version := "0.1.9-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.0-RC1-bin-20220530-d6e3d12-NIGHTLY"

ThisBuild / organization := "space.littleinferno"

lazy val root = (project in file("."))
  .settings(name := "muffin")
  .dependsOn(core, client, http, `http-sttp`, app)
  .aggregate(core, client, http, `http-sttp`, app)


lazy val modules = file("modules")

lazy val http = project
  .in(modules / "http")

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

lazy val testing = project
  .in(modules / "testing")
  .aggregate(core, client, http, `http-sttp`, app)
  .dependsOn(core, client, http, `http-sttp`, app)
  .settings(publish / skip := true)

ThisBuild / scalacOptions += "-source:future"

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.1"

ThisBuild / organization := "space.littleinferno"


lazy val root = (project in file("."))
  .settings(
    name := "muffin"
  )
  .dependsOn(
    core,
    client,
    http,
    `http-sttp`
  )

lazy val modules = file("modules")

lazy val http = project
  .in(modules / "http")

lazy val core = project
  .in(modules / "core")

lazy val client = project
  .in(modules / "client")
  .dependsOn(core, http)

lazy val integration = modules / "integration"

lazy val `http-sttp` = project
  .in(integration / "http-sttp")
  .dependsOn(http)

lazy val testing = project
  .in(modules / "testing")
  .aggregate(
    core,
    client,
    http,
    `http-sttp`
  )
  .dependsOn(
    core,
    client,
    http,
    `http-sttp`
  )
  .settings(
    libraryDependencies += Dependencies.zioCats,
    publish / skip := true
  )


inThisBuild(
  Seq(
    organization := "ru.tinkoff",
    homepage := Some(url("https://github.com/Tinkoff/muffin")),
    description := "Mattermost API for Scala 3",
    developers := List(
      Developer(
        "little-inferno",
        "Danil Zasypkin",
        "d.zasypkin@tinkoff.ru",
        url("https://github.com/little-inferno")
      )
    ),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
  )
)

addCommandAlias("fixCheck", "scalafmtCheckAll")

val commonSettings = Seq(
  version := "0.2.0",
  scalaVersion := "3.2.0",
  Compile / packageDoc / publishArtifact := false,
  Compile / packageSrc / publishArtifact := false,
  Compile / doc / sources := Seq.empty,

  scalacOptions ++= Seq(
    "-explain",
    "-deprecation",
    "-no-indent",
    "-old-syntax"
  ),

  publishMavenStyle := true,
  credentials ++= Option(Path.userHome / ".sbt" / "sonatype_credential")
    .filter(_.exists)
    .map(Credentials.apply),
)

val skipPublish = Seq(
  publish / skip := true
)

lazy val root = (project in file("."))
  .settings(
    name := "muffin",
    skipPublish,
    commonSettings
  )
  .aggregate(
    `muffin-core`,
    `muffin-sttp-http-interop`,
    `muffin-circe-json-interop`,
    `muffin-zio-json-interop`,
    `muffin-zio-http-interop`,
    `muffin-http4s-http-interop`
  )

lazy val modules = file("modules")

lazy val `muffin-core` = project
  .in(modules / "core")
  .settings(commonSettings)
  .settings(libraryDependencies ~= (_.map(_.excludeAll(ExclusionRule(organization = "org.scala-lang.modules", name = "scala-collection-compat_2.13")))))

val TestAndCompile = config("test->test;compile->compile")

lazy val integration = modules / "integration"

lazy val `muffin-sttp-http-interop` = project
  .in(integration / "sttp-http-interop")
  .settings(commonSettings)
  .dependsOn(`muffin-core`)

lazy val `muffin-http4s-http-interop` = project
  .in(integration / "http4s-http-interop")
  .settings(commonSettings)
  .dependsOn(`muffin-core`)

lazy val `muffin-zio-http-interop` = project
  .in(integration / "zio-http-interop")
  .settings(commonSettings)
  .dependsOn(`muffin-core`)

lazy val `muffin-circe-json-interop` = project
  .in(integration / "circe-json-interop")
  .settings(commonSettings)
  .dependsOn(`muffin-core` % TestAndCompile)

lazy val `muffin-zio-json-interop` = project
  .in(integration / "zio-json-interop")
  .settings(commonSettings)
  .dependsOn(`muffin-core` % TestAndCompile)

lazy val examples = modules / "examples"

lazy val `muffin-http4s-sttp-circe-example` = project
  .in(examples / "http4s-sttp-circe")
  .settings(commonSettings)
  .dependsOn(`muffin-core`, `muffin-sttp-http-interop`, `muffin-http4s-http-interop`, `muffin-circe-json-interop`)
  .settings(skipPublish)

lazy val `muffin-zhttp-zjson-example` = project
  .in(examples / "zhttp-zjson")
  .settings(commonSettings)
  .dependsOn(`muffin-core`, `muffin-zio-http-interop`, `muffin-zio-json-interop`)
  .settings(skipPublish)

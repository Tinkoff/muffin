import sbt._

object Dependencies {

  object Version {
    val sttp = "3.8.15"

    val circe = "0.14.3"

    val http4s = "0.23.22"

    object zio {
      val http = "3.0.0-RC2"
      val json = "0.6.0"
      val core = "2.0.13"
    }

    val fs2 = "3.6.1"

    object cats {
      val core   = "2.9.0"
      val effect = "3.4.10"
    }

    val scalatest = "3.2.15"
  }

  val sttp = "com.softwaremill.sttp.client3" %% "core" % Version.sttp

  object http4s {
    val core = "org.http4s" %% "http4s-core" % "0.23.22"
    val dsl  = "org.http4s" %% "http4s-dsl"  % "0.23.22"
  }

  object circe {
    val core   = "io.circe" %% "circe-core"   % Version.circe
    val parser = "io.circe" %% "circe-parser" % Version.circe
  }

  object zio {
    val json = "dev.zio" %% "zio-json" % Version.zio.json
    val http = "dev.zio" %% "zio-http" % Version.zio.http
    val core = "dev.zio" %% "zio"      % Version.zio.core
  }

  val fs2 = "co.fs2" %% "fs2-core" % Version.fs2

  object cats {
    val core   = "org.typelevel" %% "cats-core"   % Version.cats.core
    val effect = "org.typelevel" %% "cats-effect" % Version.cats.effect
  }

  object scalatest {
    val core        = "org.scalatest" %% "scalatest-core"        % Version.scalatest % Test
    val featureSpec = "org.scalatest" %% "scalatest-featurespec" % Version.scalatest % Test
  }

}

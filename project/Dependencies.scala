import sbt._

object Dependencies extends process.Deps with compilation.Deps with test.Deps {
//  val DependencyOverrides: Seq[ModuleID] =
//    Seq(
//      slf4j,
//      cats.core,
//      cats.effect,
//      "org.scala-lang.modules" %% "scala-collection-compat" % "2.2.0",
//      magnolia,
//      refined,
//      "org.typelevel"   %% "cats-free" % "2.2.0",
//      "net.java.dev.jna" % "jna"       % "5.4.0",
//      typedSchema.akka
//    )
}

object process extends Libs {
  object version {
    val sttp = "3.5.1"
    val circe = "0.14.1"
  }

  trait Deps {

    object sttp {
      val core = "com.softwaremill.sttp.client3" %% "core" % version.sttp
      val zio =
        "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % version.sttp
    }

    object circe {
      val core = "io.circe" %% "circe-core" % version.circe
      val generic = "io.circe" %% "circe-generic" % version.circe
      val parser = "io.circe" %% "circe-parser" % version.circe
    }

    val zioCats = "dev.zio" %% "zio-interop-cats" % "3.3.0-RC2"
    //    lazy val enumeratum  = "com.beachape"               %% "enumeratum"        % version.enumeratum
//    lazy val ficus       = "com.iheart"                 %% "ficus"             % version.ficus excludeAll ficusExclude
//    lazy val quicklens   = "com.softwaremill.quicklens" %% "quicklens"         % version.quicklens
//    lazy val ldap        = "com.unboundid"               % "unboundid-ldapsdk" % version.ldap
//    lazy val logback     = "ch.qos.logback"              % "logback-classic"   % version.logback excludeModule slf4j
//    lazy val swaggerUi   = "org.webjars.npm"             % "swagger-ui-dist"   % version.swaggerUi
//    lazy val refined     = "eu.timepit"                 %% "refined"           % version.refined

  }
}

object compilation extends Libs {
  object version {
//    val betterFor     = "0.3.1"
//    val kindProjector = "0.13.2"
//    val scalaReflect  = "2.13.3"
//
//    object scalaFix {
//      val semanticDB  = "4.3.0"
//      val ruleDisable = "0.1.4.1"
//      val sortImports = "0.3.2"
//    }
  }
//
  trait Deps {
//    lazy val betterFor     = "com.olegpy"    %% "better-monadic-for" % version.betterFor
//    lazy val kindProjector = "org.typelevel" %% "kind-projector"     % version.kindProjector cross CrossVersion.full
//    lazy val scalaReflect  = "org.scala-lang" % "scala-reflect"      % version.scalaReflect
//
//    object scalaFix {
//      val semanticDB  = "org.scalameta"       % "semanticdb-scalac" % version.scalaFix.semanticDB cross CrossVersion.full
//      val ruleDisable = "com.github.vovapolu" % "scaluzzi_2.12"     % version.scalaFix.ruleDisable
//      val sortImports = "com.nequissimus"     % "sort-imports_2.12" % version.scalaFix.sortImports
//    }
  }
}

object test extends Libs {
  object version {
//    val scalaTest      = "3.1.0"
//    val testcontainers = "0.39.5"
//    val postgresql     = "1.15.3"
//    val flyway         = "6.1.4"
//    val scalamock      = "4.4.0"
  }
//
  trait Deps {
//    lazy val scalaTest      = "org.scalatest"     %% "scalatest"            % version.scalaTest      % "it, test"
//    lazy val scalastic      = "org.scalactic"     %% "scalactic"            % version.scalaTest      % "it, test"
//    lazy val testcontainers = "com.dimafeng"      %% "testcontainers-scala" % version.testcontainers % "it"
//    lazy val postgresql     = "org.testcontainers" % "postgresql"           % version.postgresql     % "it"
//    lazy val flyway         = "org.flywaydb"       % "flyway-core"          % version.flyway         % "it"
//    lazy val scalamock      = "org.scalamock"     %% "scalamock"            % version.scalamock      % "it, test"
  }
}

trait Libs extends ModulesSyntax {
  val version: AnyRef

//  def tofuExclude: List[ExclusionRule] =
//    ExclusionRule("me.moocar", "logback-gelf") :: ExclusionRule("me.moocar", "socket-encoder-appender") :: Nil
//
//  def circeExclude: List[ExclusionRule] =
//    ExclusionRule("io.circe", "circe-core_2.13") :: ExclusionRule("io.circe", "circe-generic_2.13") :: Nil
//
//  def typedSchemaExclude: List[ExclusionRule] =
//    ExclusionRule("ru.tinkoff", "typed-schema-swagger_2.13") :: ExclusionRule(
//      "ru.tinkoff",
//      "typed-schema-param_2.13"
//    ) :: Nil
//
//  def akkaHttpCirceExclude: List[ExclusionRule] = ExclusionRule("de.heikoseeberger", "akka-http-circe_2.13") :: Nil
//
//  def enumerantumExclude: List[ExclusionRule] = ExclusionRule("com.beachape", "enumeratum_2.13") :: Nil
//
//  def ficusExclude: List[ExclusionRule] = ExclusionRule("com.typesafe", "config") :: Nil
//
//  def akkaExclude: List[ExclusionRule] = ExclusionRule("com.typesafe.akka", "akka-http_2.13") :: Nil
}

trait ModulesSyntax {
  implicit class ScopeSyntax(val modules: Seq[ModuleID]) {
    def in(scope: String): Seq[ModuleID] = modules map (_ % scope)
  }

  final implicit def toModuleOps(module: ModuleID): ModulesSyntax.ModuleOps =
    new ModulesSyntax.ModuleOps(module)
}

object ModulesSyntax {
  final class ModuleOps(private val id: ModuleID) extends AnyVal {
    def excludeModule(module: ModuleID*): ModuleID =
      excludeAll(
        module.map(m =>
          ExclusionRule(
            m.organization,
            if (m.crossVersion == Binary()) s"${m.name}_2.13" else m.name
          )
        )
      )

    def excludeAll(rules: Seq[ExclusionRule])(implicit
        d: DummyImplicit
    ): ModuleID = id.excludeAll(rules: _*)
  }
}

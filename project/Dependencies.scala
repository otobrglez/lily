import sbt.*
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport.*

object Dependencies {
  type Version = String
  type Modules = Seq[ModuleID]

  object Versions {
    val circe: Version                 = "0.14.13"
    val enumeratum: Version            = "1.7.0"
    val scalaTest: Version             = "3.2.19"
    val sentry: Version                = "8.12.0"
    val sentryAgent: Version           = sentry
    val sentryLogback: Version         = sentry
    val zio: Version                   = "2.1.18"
    val zioConfig: Version             = "4.0.4"
    val zioHttp: Version               = "3.3.0"
    val zioLogging: Version            = "2.5.0"
    val zioMetrics: Version            = "2.3.1"
    val zioQuery: Version              = "0.7.7"
    val zioSchema: Version             = "1.7.2"
    val izumiReflect: Version          = "3.0.1"
    val scalaCollectionCompat: Version = "2.13.0"
    val borer: Version                 = "1.16.1"
    val jsoup: Version                 = "1.20.1"
  }

  object JS {
    lazy val coreJS = Def.setting(
      Seq(
        "io.github.cquiroz" %%% "scala-java-time" % "2.6.0",
        "org.scala-js"      %%% "scalajs-dom"     % "2.8.0"
      )
    )

    lazy val json = Def.setting(
      Seq(
        "io.circe" %%% "circe-core",
        "io.circe" %%% "circe-generic",
        "io.circe" %%% "circe-parser"
      ).map(_ % Versions.circe)
    )

    lazy val cbor = Def.setting(
      Seq(
        "io.bullet" %%% "borer-core",
        "io.bullet" %%% "borer-derivation",
        "io.bullet" %%% "borer-compat-circe"
      ).map(_ % Versions.borer)
    )
  }

  lazy val zio: Modules = Seq(
    "dev.zio" %% "zio",
    "dev.zio" %% "zio-streams"
  ).map(_ % Versions.zio) ++ Seq(
    "dev.zio" %% "zio-test",
    "dev.zio" %% "zio-test-sbt",
    "dev.zio" %% "zio-test-magnolia"
  ).map(_ % Versions.zio % Test) ++ Seq(
    "dev.zio" %% "zio-prelude" % "1.0.0-RC40",
    "dev.zio" %% "zio-cli"     % "0.7.2"
  ) ++ Seq(
    "dev.zio" %% "zio-logging",
    "dev.zio" %% "zio-logging-slf4j2"
  ).map(_ % Versions.zioLogging) ++ Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.18"
  ) ++ Seq(
    "dev.zio" %% "zio-schema",
    "dev.zio" %% "zio-schema-json",
    "dev.zio" %% "zio-schema-zio-test",
    "dev.zio" %% "zio-schema-derivation"
    // "org.scala-lang" % "scala-reflect"  % scalaVersion.value % "provided" // Needed ?
  ).map(_ % Versions.zioSchema) ++ Seq(
    "dev.zio" %% "zio-metrics-connectors",
    "dev.zio" %% "zio-metrics-connectors-prometheus"
  ).map(_ % Versions.zioMetrics) ++ Seq(
    "dev.zio" %% "zio-json-yaml" % "0.7.43"
  ) ++ Seq(
    "eu.timepit" %% "refined" % "0.11.3"
  ) ++ Seq(
    "dev.zio" %% "zio-http"         % Versions.zioHttp,
    "dev.zio" %% "zio-http-testkit" % Versions.zioHttp % Test
  ) ++ Seq(
    "dev.zio" %% "zio-config",
    "dev.zio" %% "zio-config-magnolia",
    "dev.zio" %% "zio-config-typesafe",
    "dev.zio" %% "zio-config-refined"
  ).map(_ % Versions.zioConfig) ++ Seq(
    "dev.zio" %% "zio-query"
  ).map(_ % Versions.zioQuery)

  lazy val logging: Modules = Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.18"
  ) ++ Seq(
    "io.sentry" % "sentry-logback" % Versions.sentryLogback
  )

  lazy val enumeratum: Modules = Seq(
    "com.beachape" %% "enumeratum",
    "com.beachape" %% "enumeratum-circe"
  ).map(_ % Versions.enumeratum)

  lazy val json: Modules = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % Versions.circe)

  lazy val cbor: Modules = Seq(
    "io.bullet" %% "borer-core",
    "io.bullet" %% "borer-derivation",
    "io.bullet" %% "borer-compat-circe"
  ).map(_ % Versions.borer)

  lazy val jwt: Modules = Seq(
    "com.github.jwt-scala" %% "jwt-core",
    "com.github.jwt-scala" %% "jwt-circe"
  ).map(_ % "10.0.4")

  lazy val markdownAndJsoup: Modules = Seq(
    "org.commonmark" % "commonmark" % "0.24.0",
    "org.jsoup"      % "jsoup"      % Versions.jsoup
  )

  lazy val projectResolvers: Seq[MavenRepository] = Seq(
    // Resolver.sonatypeOssRepos("snapshots"),
    "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
    "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
    "Java.net Maven2 Repository" at "https://download.java.net/maven/2/",
    "JitPack".at("https://jitpack.io")
  )
}

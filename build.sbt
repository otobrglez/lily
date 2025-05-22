import Dependencies.*
import com.typesafe.sbt.packager.docker.{Cmd, DockerPermissionStrategy}
import sbtassembly.AssemblyKeys.assembly
import sbtassembly.{MergeStrategy, PathList}
val scala3Version = "3.7.0"

ThisBuild / dynverVTagPrefix  := false
ThisBuild / dynverSeparator   := "-"
ThisBuild / scalaVersion      := scala3Version
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val root = project
  .enablePlugins(BuildInfoPlugin, JavaAgent, JavaAppPackaging, LauncherJarPlugin, DockerPlugin)
  .in(file("."))
  .settings(
    buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.pinkstack.lily"
  )
  .settings(
    name         := "lily",
    scalaVersion := scala3Version,
    libraryDependencies ++= {
      zio ++ logging ++ json ++ jwt ++ enumeratum ++ redis ++ db
    },
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-explain",
      "-Yretain-trees",
      "-Xmax-inlines:100",
      "-Ximplicit-search-limit:150000",
      "-language:implicitConversions",
      "-Wunused:all"
    )
  )
  .settings(
    javaAgents += "io.sentry" % "sentry-opentelemetry-agent" % Versions.sentryAgent
  )
  .settings(
    assembly / mainClass             := Some("com.pinkstack.lily.apps.Main"),
    assembly / assemblyJarName       := "lily.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class")                        =>
        MergeStrategy.discard
      case PathList("META-INF", "jpms.args")                    =>
        MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") =>
        MergeStrategy.first
      case PathList("deriving.conf")                            =>
        MergeStrategy.last
      case PathList(ps @ _*) if ps.last endsWith ".class"       => MergeStrategy.last
      case x                                                    =>
        val old = (assembly / assemblyMergeStrategy).value
        old(x)
    }
  )
  .settings(
    dockerExposedPorts ++= Seq(4444),
    dockerExposedUdpPorts    := Seq.empty[Int],
    dockerUsername           := Some("otobrglez"),
    dockerUpdateLatest       := true,
    dockerRepository         := Some("ghcr.io"),
    dockerBaseImage          := "azul/zulu-openjdk:21-jre-headless-latest",
    Docker / daemonUserUid   := None,
    Docker / daemonUser      := "root",
    dockerPermissionStrategy := DockerPermissionStrategy.None,
    packageName              := "goo",
    dockerCommands           := dockerCommands.value.flatMap {
      case cmd @ Cmd("WORKDIR", _) =>
        List(
          Cmd("LABEL", "maintainer=\"Oto Brglez <otobrglez@gmail.com>\""),
          Cmd(
            "LABEL",
            "org.opencontainers.image.url=https://github.com/otobrglez/lily"
          ),
          Cmd(
            "LABEL",
            "org.opencontainers.image.source=https://github.com/otobrglez/lily"
          ),
          Cmd("ENV", "PORT=4444"),
          Cmd("ENV", s"LILY_VERSION=${version.value}"),
          cmd
        )
      case other                   => List(other)
    }
  )

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")
addCommandAlias("fix", ";scalafixAll")

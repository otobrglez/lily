import Dependencies.*
import com.typesafe.sbt.packager.docker.{Cmd, DockerPermissionStrategy}
import sbtassembly.AssemblyKeys.assembly
import sbtassembly.{MergeStrategy, PathList}
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import sbtcrossproject.CrossPlugin.autoImport.*
import sbtcrossproject.CrossProject
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import sbt.io.IO

val scala3Version = "3.7.0"

ThisBuild / organization      := "dev.lily"
ThisBuild / dynverVTagPrefix  := false
ThisBuild / dynverSeparator   := "_"
ThisBuild / scalaVersion      := scala3Version
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / scalaVersion      := scala3Version

def copyAll(location: File, outDir: File): List[File] = IO.listFiles(location).toList.map { file =>
  val (name, ext) = file.baseAndExt
  val out         = outDir / (name + "." + ext)
  IO.copyFile(file, out)
  out
}

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .enablePlugins(BuildInfoPlugin)
  .in(file("core"))
  .settings(
    name             := "core",
    scalaVersion     := scalaVersion.value,
    buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "dev.lily.info"
    /*
    libraryDependencies ++= List(
        "dev.zio"                %%% "izumi-reflect"           % Versions.izumiReflect,
       "org.scala-lang.modules" %%% "scala-collection-compat" % Versions.scalaCollectionCompat
    )

     */
  )
  .jsSettings(
    libraryDependencies ++= { JS.coreJS.value ++ JS.json.value ++ JS.cbor.value }
  )
  .jvmSettings(
    libraryDependencies ++= { zio ++ json ++ cbor },
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(core.js)
  .settings(
    name                            := "frontend",
    scalaVersion                    := scalaVersion.value,
    scalaJSUseMainModuleInitializer := true,
    /*
    scalaJSMainModuleInitializer    := Some(
      org.scalajs.linker.interface.ModuleInitializer
        .mainMethod("dev.lilly.fe.Main", "main")
    ), */

    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    libraryDependencies ++= { zio ++ JS.coreJS.value ++ JS.json.value },
    Compile / fastOptJS / moduleName    := "lily",
    Compile / fullOptJS / moduleName    := "lily",
    Compile / fastOptJS / artifactPath  := baseDirectory.value / "lily.js",
    Compile / fullOptJS / artifactPath  := baseDirectory.value / "lily.js",
    Compile / fastLinkJS / artifactPath := baseDirectory.value / "target" / "lily.js",
    Compile / fullLinkJS / artifactPath := baseDirectory.value / "target" / "lily.js"
  )

lazy val backend = (project in file("backend"))
  .dependsOn(core.jvm)
  .dependsOn(frontend)
  .settings(
    name                 := "backend",
    scalaVersion         := scalaVersion.value,
    libraryDependencies ++= { zio ++ jwt ++ logging ++ markdownAndJsoup },
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    assembly / mainClass := Some("dev.lily.apps.Main")
  )
  .settings(
    Compile / resourceGenerators += {
      Def.task[Seq[File]] {
        val log = streams.value.log
        log.info("🔥 Compiling and injecting frontend bundle. 🔥")

        copyAll(
          frontendOptBundle.value,
          (Compile / resourceManaged).value / "assets"
        )
      }
    }
  )

lazy val frontendBundle = taskKey[File]("")
ThisBuild / frontendBundle := Def.task {
  val res = (frontend / Compile / fastLinkJS).value
  (frontend / Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
}.value

lazy val frontendOptBundle = taskKey[File]("")
ThisBuild / frontendOptBundle := Def.task {
  val res = (frontend / Compile / fullLinkJS).value
  (frontend / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
}.value

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")
addCommandAlias("fix", ";scalafixAll")

resolvers ++= Dependencies.projectResolvers

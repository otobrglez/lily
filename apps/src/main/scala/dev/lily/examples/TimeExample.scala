package dev.lily.examples

import dev.lily.LiveView
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import zio.http.Path
import zio.stream.ZStream
import zio.{durationInt, Schedule, ZIO}

import java.time.{LocalDateTime, ZoneId}

object TimeExample extends LiveView[Any, LocalDateTime]:
  def initialState =
    ZStream
      .repeatZIO(ZIO.succeed(LocalDateTime.now(ZoneId.of("CET"))))
      .schedule(Schedule.spaced(10.millis))
      .changes

  def on(s: LocalDateTime): TimeExample.Handler = emptyHandler

  def render(time: LocalDateTime, path: Path): ZIO[Any, Throwable, Html] = ZIO.succeed:
    Examples.layout(Some("Time example"), Some(path))(
      h1("Lily - Time example"),
      p("This example shows the live time on the server."),
      p(s"Server time: $time").attr("style" -> "font-size: 15pt"),
      h2("Memory stats"),
      ul(MemoryStats.stats.toSeq.map((k, v) => li(span(k + " "), span(v))).toList*)
    )

object MemoryStats:
  def stats: Map[String, String] =
    val runtime     = Runtime.getRuntime
    // Total memory currently in use by JVM (bytes)
    val totalMemory = runtime.totalMemory
    // Free memory within the total memory (bytes)
    val freeMemory  = runtime.freeMemory
    // Maximum memory the JVM will attempt to use (bytes)
    val maxMemory   = runtime.maxMemory
    // Memory currently used by the application
    val usedMemory  = totalMemory - freeMemory

    val usedMB  = usedMemory / (1024.0 * 1024.0)
    val freeMB  = freeMemory / (1024.0 * 1024.0)
    val totalMB = totalMemory / (1024.0 * 1024.0)
    val maxMB   = maxMemory / (1024.0 * 1024.0)

    Map(
      "Used Memory"  -> f"$usedMB%.2f MB",
      "Free Memory"  -> f"$freeMB%.2f MB",
      "Total Memory" -> f"$totalMB%.2f MB",
      "Max Memory"   -> f"$maxMB%.2f MB"
    )

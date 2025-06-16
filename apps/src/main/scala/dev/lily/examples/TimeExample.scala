package dev.lily.examples

import dev.lily.LiveView
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import zio.http.Path
import zio.stream.ZStream
import zio.{durationInt, Schedule, ZIO}

import java.time.{LocalDateTime, ZoneId}

object TimeExample extends LiveView[Any, LocalDateTime]:
  private val updateInterval = 100.millis
  private val timeZone       = ZoneId.of("CET")

  def initialState =
    ZStream
      .repeatZIO(ZIO.succeed(LocalDateTime.now(timeZone)))
      .schedule(Schedule.spaced(updateInterval))
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
  private val mbDivisor = 1024.0 * 1024.0

  def stats: Map[String, String] =
    val runtime     = Runtime.getRuntime
    val totalMemory = runtime.totalMemory
    val freeMemory  = runtime.freeMemory
    val maxMemory   = runtime.maxMemory
    val usedMemory  = totalMemory - freeMemory

    Map(
      "Used Memory"  -> f"${usedMemory / mbDivisor}%.2f MB",
      "Free Memory"  -> f"${freeMemory / mbDivisor}%.2f MB",
      "Total Memory" -> f"${totalMemory / mbDivisor}%.2f MB",
      "Max Memory"   -> f"${maxMemory / mbDivisor}%.2f MB"
    )

package dev.lily.examples

import dev.lily.ClientEvent.on
import dev.lily.HTMLOps.{*, given}
import dev.lily.LiveView
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import zio.http.Path
import zio.stream.ZStream
import zio.{Task, ZIO}

object CounterExample extends LiveView[Any, Int]:
  def state = ZStream.fromZIO(ZIO.succeed(0))

  def on(s: Int) =
    case on("increment" -> _) => ZIO.succeed(s + 1)
    case on("decrement" -> _) => ZIO.succeed(s - 1)
    case _                    => ZIO.succeed(s)

  def render(n: Int, path: Path): Task[Html] = ZIO.succeed:
    Examples.layout(Some("Simple Counter Example"), Some(path))(
      h1("Lily - Counter example"),
      div(p(s"Counter is now: $n")),
      div(p(s"Plus two is ${n + 2}")),
      div(p(s"Some math: ${n * 1.2 * Math.PI}")),
      div(
        button("Increment").on("click" -> "increment"),
        button("Decrement").on("click" -> "decrement")
      )
    )

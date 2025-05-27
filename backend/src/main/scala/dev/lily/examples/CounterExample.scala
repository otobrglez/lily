package dev.lily.examples

import dev.lily.ClientEvent.on
import dev.lily.HTMLOps.{*, given}
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.{ClientEvent, LiveView}
import zio.http.Path
import zio.stream.ZStream
import zio.{Task, ZIO}

object CounterExample extends LiveView[Any, Int]:
  def state = ZStream.fromZIO(ZIO.succeed(0))

  override def onEvent(state: Int, event: ClientEvent): Task[Int] = event match
    case on("increment" -> _) => ZIO.succeed(state + 1)
    case on("decrement" -> _) => ZIO.succeed(state - 1)
    case _                    => ZIO.succeed(state)

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

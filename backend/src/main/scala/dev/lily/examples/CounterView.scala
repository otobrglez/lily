package dev.lily.examples

import dev.lily.HTMLOps.{*, given}
import dev.lily.WebSocketOps.given
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.{ClientEvent, DomChanged, LiveView}
import zio.ZIO.logInfo
import zio.{Task, UIO, ZIO}

// TODO: Can't access current dom from here?
object CounterView extends LiveView[Int]:
  def state: UIO[Int] = ZIO.succeed(0)

  def handle(state: Int, event: ClientEvent): Task[Int] = event match
    case ClientEvent(_, "increment", _, _)              => ZIO.succeed(state + 1)
    case ClientEvent(_, "decrement", _, _)              => ZIO.succeed(state - 1)
    case ClientEvent(_, "changeSlider", _, Some(value)) =>
      for n <- ZIO.fromOption(value.toIntOption).orElseFail(new RuntimeException("Boom"))
      yield n
    case _                                              => ZIO.succeed(state)

  def render(n: Int): Task[Html] = ZIO.succeed:
    html(
      head(title("Hello counter")),
      attachLIIDs(
        body(
          h1("Lily - Counter example"),
          p(a("&laquo; Back to examples").withAttr("href", "/")),
          div(p(s"Counter is now: ${n}")),
          div(p(s"Plus two is ${(n + 2)}")),
          div(p(s"Some math: ${n * 1.2}")),
          div(
            button("Increment").on("click" -> "increment"),
            button("Decrement").on("click" -> "decrement")
          ),
          div(
            p("Sliders maybe?"),
            input()
              .withAttr("type" -> "range", "min" -> "0", "max" -> "100", "value" -> n.toString)
              .on("input" -> "changeSlider"),
            p(
              s"Value is ${n}"
            )
          )
        )
      )
    )

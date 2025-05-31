package dev.lily.apps

import dev.lily.HTMLOps.given
import dev.lily.examples.Examples
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.{examples, LiveView}
import zio.*
import zio.Runtime.{removeDefaultLoggers, setConfigProvider}
import zio.http.*
import zio.http.codec.PathCodec.*
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    setConfigProvider(ConfigProvider.envProvider) >>> removeDefaultLoggers >>> SLF4J.slf4j

  private val routes =
    Routes(
      Method.GET / Root            -> handler(
        Response.html(
          Examples.layout(
            pageTitle = Some("Welcome!"),
            moreCss = Some("""
                             |span.big-title { font-size: 50pt; } """.stripMargin)
          )(
            h1(span("This is Lily. 🌸").klass("big-title"), " Hello! "),
            p("Lily is a tiny framework for rapid and easy development of live/real-time applications is Scala."),
            p(
              "Lily is build around ",
              a("ZIO HTTP").attr("href"    -> "https://ziohttp.com/"),
              " and ",
              a("ZIO Streams").attr("href" -> "https://zio.dev/reference/stream/"),
              " and offloads the heavy lifting to the server side!"
            ),
            p("It's a work in progress, so expect things to change."),
            p("If you want to see some examples, check out the links below:"),
            ul(
              li(a("Table example.").attr("href" -> "/tables")),
              li(a("Live server time example.").attr("href" -> "clock")),
              li(a("Markdown editor").attr("href" -> "markdown")),
              li(a("Counter example.").attr("href" -> "/counter-v2"))
            )
          )
        )
      ),
      Method.GET / "api" / "hello" -> handler(Response.text("Hello, World!"))
    )
      ++ LiveView.route(Path.empty / "counter-v2", examples.CounterExample)
      ++ LiveView.routeZIO(Path.empty / "tables", examples.TableExample.make)
      ++ LiveView.route(Path.empty / "clock", examples.TimeExample)
      ++ LiveView.routeZIO(Path.empty / "markdown", examples.MarkdownEditor.make)

  private val staticMiddleware = Middleware.serveResources(Path.empty / "static", "assets")
  private val app              = routes @@ staticMiddleware @@ Middleware.debug

  def run = Server
    .serve(app)
    .provide(Server.defaultWithPort(3334))

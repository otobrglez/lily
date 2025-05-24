package dev.lily.apps

import dev.lily.HTMLOps.given
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

  private val routes = Routes(
    Method.GET / Root            -> handler(
      Response.html(
        html(
          head(
            title("Lily"),
            meta().withAttr("charset" -> "utf-8"),
            meta().withAttr("name"    -> "viewport", "content" -> "width=device-width, initial-scale=1")
          ),
          body(
            h1("Hello from Lily! 👋"),
            div(
              ul(
                li(a("Live Clock Demo").withAttr("href", "/live-clock")),
                li(a("Counter demo v2").withAttr("href", "/counter-v2"))
              )
            ).withClass("main")
          )
        )
      )
    ),
    Method.GET / "api" / "hello" -> handler(Response.text("Hello, World!"))
  )
    ++ LiveView.route(Path.empty / "counter-v2", examples.CounterView)

  private val staticMiddleware = Middleware.serveResources(Path.empty / "static", "assets")
  private val app              = routes @@ staticMiddleware @@ Middleware.debug

  def run = Server.serve(app).provide(Server.defaultWithPort(3334))

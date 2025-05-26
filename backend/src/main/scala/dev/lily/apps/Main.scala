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
            meta().attr("charset" -> "utf-8"),
            meta().attr("name"    -> "viewport", "content" -> "width=device-width, initial-scale=1")
          ),
          body(
            h1("Hello from Lily! 👋"),
            div(
              ul(
                li(a("Counter example").attr("href", "/counter-v2")),
                li(a("Table example").attr("href", "/tables"))
              )
            ).klass("main")
          )
        )
      )
    ),
    Method.GET / "api" / "hello" -> handler(Response.text("Hello, World!"))
  )
    ++ LiveView.route(Path.empty / "counter-v2", examples.CounterExample)
    ++ LiveView.route(Path.empty / "tables", examples.TableExample)

  private val staticMiddleware = Middleware.serveResources(Path.empty / "static", "assets")
  private val app              = routes @@ staticMiddleware @@ Middleware.debug

  def run = Server.serve(app).provide(Server.defaultWithPort(3334))

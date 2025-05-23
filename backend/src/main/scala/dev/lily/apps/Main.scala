package dev.lily.apps

import dev.lilly.HTMLZ
import zio.ZIOAppDefault
import zio.ZIO.logInfo
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.trailing
import zio.http.template.*

object Main extends ZIOAppDefault:

  /*
  private def program =
    logInfo("Hello, world from backend") *>
      logInfo(HTMLZ.sayHello("Oto Brglez"))

  def run = program
   */

  // Define the routes for your API
  val apiRoutes = Routes(
    Method.GET / Root            -> handler(
      Response.html(
        Html.raw(
          "<html><head><title>lily</title>" +
            "<script src=\"/static/main.js\"></script>" +
            "</head>" +
            "<body><h1>Hello, World!</h1></body>" +
            "</html>"
        )
      )
    ),
    Method.GET / "api" / "hello" -> handler(Response.text("Hello, World!"))
  )

  // Define static assets middleware
  // This will serve files from the "assets" directory in resources at the URL path /static
  val staticMiddleware = Middleware.serveResources(Path.empty / "static", "assets")

  // Combine API routes with static middleware
  val app = apiRoutes @@ staticMiddleware

  // Run the server
  override def run = Server.serve(app).provide(Server.defaultWithPort(3334))

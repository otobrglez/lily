package dev.lily

import dev.lily.HTMLOps.{*, given}
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import zio.*
import zio.ZIO.{logErrorCause, logInfo}
import zio.http.*
import zio.http.ChannelEvent.{ExceptionCaught, Read, UserEventTriggered}

import java.time.LocalDateTime

final case class Live private ()
object Live:
  private val utc  = java.time.ZoneOffset.UTC
  private def time = LocalDateTime.now(utc)

  private val socketApp: WebSocketApp[Any] = Handler.webSocket: channel =>
    channel.receiveAll:
      case Read(WebSocketFrame.Text("end")) => channel.shutdown

      case Read(WebSocketFrame.Text(text)) =>
        logInfo(s"Got: ${text}") *> channel.send(Read(WebSocketFrame.Text("Sending back from server. Got: " + text)))

      case ExceptionCaught(ex) => logErrorCause("Caught websocket exception.", Cause.fail(ex))

      case UserEventTriggered(event) =>
        logInfo("Replied") *> channel.send(Read(WebSocketFrame.Text(s"Got event: ${event}. Hello")))

      case other => logInfo(s"Got: ${other}")

  private def paths(main: Path => Path): (Path, Path) =
    val livePath = main(Path.empty)
    livePath -> livePath / "ws"

  def route(path: Path => Path = _ => Path.empty): Routes[Any, Nothing] =
    val (mainPath, livePath) = paths(path)
    Routes(
      RoutePattern(Method.GET, livePath) -> handler(socketApp.toResponse),
      RoutePattern(Method.GET, mainPath) -> handler:
        Response.html(
          Html.attachLIIDs(
            html(
              head(
                title("Lily - The Live Clock Demo"),
                meta().withAttr("charset" -> "utf-8"),
                meta().withAttr("name"    -> "viewport", "content" -> "width=device-width, initial-scale=1"),
                script().withAttr("src"   -> "/static/main.js")
              ),
              body(
                h1("Lily - The Live Clock Demo. 🚀"),
                p(a("&laquo; Back to examples").withAttr("href", "/")),
                p(
                  h3("Tools"),
                  ul(
                    li(a("Increment +").on("click", "increment")),
                    li(a("Decrement -").on("click", "decrement"))
                  )
                ),
                p(s"Server clock is $time").withAttr("style", "color: red;")
              ).withData(
                "li-ws-path"              -> livePath.toString,
                "li-reload-on-disconnect" -> "true",
                "li-reload-timeout"       -> "1000"
              )
            )
          )
        )
    )

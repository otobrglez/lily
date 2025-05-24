package dev.lily

import dev.lily.DomChanged
import dev.lily.DomChanged.domChangedEncoder
import dev.lily.HTMLOps.{*, given}
import dev.lily.WebSocketOps.{*, given}
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.lhtml.{Html, HtmlDiff}
import zio.Console.printLine
import zio.ZIO.{logError, logInfo}
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.Method.GET
import zio.{Ref, Task, UIO, ZIO}

import scala.util.control.NoStackTrace

trait LiveHtml:
  final def head(children: Html*): Html = Html.html(
    (children.toSeq :+ script().withAttr("src" -> "/static/main.js"))*
  )
  final def body(children: Html*): Html =
    Html
      .body(children*)
      .withData(
        "li-reload-on-disconnect" -> "true",
        "li-reload-timeout"       -> "1000",
        "li-ws-path"              -> "/counter-v2/ws" // TODO: move this
      )

trait LiveView[S] extends LiveHtml:
  protected def state: UIO[S]

  def render(state: S): Task[Html]
  def handle(state: S, event: ClientEvent): Task[S]

  final def mount: ZIO[Any, Throwable, Html] = for
    state <- state
    html  <- render(state)
  yield html

  private def parseClientEvent(
    raw: String
  )(
    action: ClientEvent => Task[Unit],
    thHandler: Throwable => UIO[Unit] = th => logError("Unknown message")
  ): UIO[Unit] =
    ZIO
      .fromEither(ClientEvent.fromProtocol(raw))
      .mapError(new RuntimeException(_) with NoStackTrace)
      .flatMap(action)
      .catchNonFatalOrDie(thHandler)

  final def run(channel: WebSocketChannel): Task[Unit] = for
    _        <- logInfo("LiveView started.")
    stateRef <- state.flatMap(Ref.make(_))
    htmlRef  <- mount.flatMap(Ref.make(_))
    _        <-
      channel.receiveAll:
        case Read(WebSocketFrame.Text(text)) =>
          parseClientEvent(text): clientEvent =>
            for
              _        <- printLine(s"Got client event: $clientEvent")
              oldState <- stateRef.get
              newState <- handle(oldState, clientEvent)
              _        <- printLine(s"New state: $newState")
              oldHtml  <- htmlRef.get
              newHtml  <- render(newState)
              domDiff  <- ZIO.attempt(HtmlDiff.unsafeOnlyBody(oldHtml, newHtml))
              _        <- stateRef.set(newState)
              _        <- htmlRef.set(newHtml)
              _        <- channel.sendString(domChangedEncoder(DomChanged(domDiff)).noSpaces)
            yield ()

        case x => logInfo(s"Got something else -> ${x}")
  yield ()

object LiveView:
  def route[S](path: Path, view: LiveView[S]): Routes[Any, Nothing] =
    val (mainPath, wsPath) = path -> path / "ws"
    Routes(
      RoutePattern(GET, wsPath)   -> handler(Handler.webSocket(channel => view.run(channel).orDie).toResponse),
      RoutePattern(GET, mainPath) -> Handler.fromZIO(view.mount.map(Response.html(_)).orDie)
    )

package dev.lily

import dev.lily.DomChanged
import dev.lily.DomChanged.domChangedEncoder
import dev.lily.HTMLOps.{*, given}
import dev.lily.WebSocketOps.{*, given}
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.lhtml.{Html, HtmlDiff}
import zio.Console.printLine
import zio.ZIO.{absolve, logError, logInfo}
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.Method.GET
import zio.stream.ZStream
import zio.{Chunk, Ref, Task, UIO, ZIO}

import scala.util.control.NoStackTrace

trait LiveHtml:
  final def head(children: Html*): Html               = Html.html(
    (children.toSeq :+ script().attr("src" -> "/static/main.js"))*
  )
  final def bodyOn(path: Path)(children: Html*): Html =
    Html
      .body(children*)
      .data(
        "li-reload-on-disconnect" -> "true",
        "li-reload-timeout"       -> "1000",
        "li-ws-path"              -> path.toString
      )

trait LiveView[-Env, S] extends LiveHtml:
  protected def state: ZStream[Env, Throwable, S]

  // Method that handles events
  def onEvent(state: S, event: ClientEvent): ZIO[Env, Throwable, S]

  // Method that actually renders the view
  def render(state: S, path: Path): ZIO[Env, Throwable, Html]

  final def mount(path: Path): ZIO[Env, Throwable, Html] = for
    maybeState <- state.runLast
    state      <- ZIO.fromOption(maybeState).orElseFail(new RuntimeException("No state found"))
    html       <- render(state, path: Path)
  yield html

  private def parseClientEvent(
    blob: Chunk[Byte]
  )(
    action: ClientEvent => ZIO[Env, Throwable, Unit],
    thHandler: Throwable => ZIO[Env, Nothing, Unit] = th => logError("Unknown message")
  ): ZIO[Env, Throwable, Unit] =
    ZIO
      .fromEither(ClientEvent.fromBinary(blob.toArray))
      .mapError(new RuntimeException(_) with NoStackTrace)
      .flatMap(action)
      .catchNonFatalOrDie(thHandler)

  final def run(channel: WebSocketChannel, path: Path): ZIO[Env, Throwable, Unit] = for
    _          <- logInfo("LiveView started.")
    maybeState <- state.runCollect.map(_.last)
    stateRef   <- Ref.make(maybeState)
    htmlRef    <- mount(path).flatMap(Ref.make(_))
    _          <-
      channel.receiveAll:
        case Read(WebSocketFrame.Binary(blob)) =>
          parseClientEvent(blob): clientEvent =>
            for
              oldState <- stateRef.get
              oldHtml  <- htmlRef.get
              newState <- onEvent(oldState, clientEvent)
              newHtml  <- render(newState, path)
              domDiff  <- ZIO.attempt(HtmlDiff.unsafeOnlyBody(oldHtml, newHtml))
              _        <- stateRef.set(newState)
              _        <- htmlRef.set(newHtml)
              _        <- channel.sendBinary(DomChanged(domDiff).toProtocol)
            yield ()

        case x => logInfo(s"Got something else: $x")
  yield ()

object LiveView:
  def route[Env, S](path: Path, view: LiveView[Env, S]): Routes[Env, Nothing] /* : Routes[Any, Nothing] */ =
    val (mainPath, wsPath) = path -> path / "ws"
    Routes(
      RoutePattern(GET, wsPath)   -> Handler.fromZIO(
        Handler.webSocket(channel => view.run(channel, wsPath).orDie).toResponse
      ),
      RoutePattern(GET, mainPath) -> Handler.fromFunctionZIO(_ => view.mount(wsPath).map(Response.html(_)).orDie)
    )

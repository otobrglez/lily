package dev.lily

import dev.lily.DomChanged
import dev.lily.DomChanged.domChangedEncoder
import dev.lily.HTMLOps.{*, given}
import dev.lily.WebSocketOps.{*, given}
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.lhtml.{Html, HtmlDiff}
import jdk.internal.net.http.common.Log.channel
import zio.Console.printLine
import zio.ZIO.{absolve, logError, logInfo, logWarning}
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.Method.GET
import zio.stream.ZStream
import zio.{Chunk, Promise, Ref, Scope, Task, UIO, ZIO}

import scala.util.control.NoStackTrace

trait LiveHtml:
  final def head(children: Html*): Html               = Html.html(
    (children.toSeq :+ script().attr("src" -> "/static/main.js"))*
  )
  
  final def bodyOn(path: Path)(children: Html*): Html = bodyOn(Some(path))(children*)

  final def bodyOn(path: Option[Path] = None)(children: Html*): Html =
    Html
      .body(children*)
      .data(
        (Map("li-reload-on-disconnect" -> "true", "li-reload-timeout" -> "1000") ++
          path.fold(Map.empty[String, String])(path => Map("li-ws-path" -> path.toString))).toSeq*
      )
  final def body(children: Html*): Html                              = bodyOn(None)(children*)

trait LiveView[-Env, S] extends LiveHtml:
  protected def state: ZStream[Env, Throwable, S]

  // Method that handles events
  def onEvent(state: S, event: ClientEvent): ZIO[Env, Throwable, S] = ZIO.succeed(state)

  // Method that actually renders the view
  def render(state: S, path: Path): ZIO[Env, Throwable, Html]

  final def mount(path: Path): ZIO[Env, Throwable, Html] = for
    maybeState <- state.runHead
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

  final def run(socket: WebSocketChannel, path: Path): ZIO[Env, Throwable, Unit] = ZIO.scoped:
    for
      _ <- logInfo(s"LiveView started on ${path}")
      // firstState <- Promise.make[Nothing, S]
      // latestState <- zio.Queue.unbounded[S]
      // latestDom   <- zio.Queue.unbounded[Html]

      currentState <- Ref.make[Option[S]](None)
      domRef       <- Ref.make[Option[Html]](None)
      promise      <- zio.Promise.make[Nothing, Unit]

      stateFib <-
        state.tap { newState =>
          for
            oldState <- currentState.get
            oldHtml  <- domRef.get
            _        <- (oldState, oldHtml) match
                          case (Some(old), Some(oldHtml)) =>
                            for
                              newHtml <- render(newState, path)
                              domDiff <- ZIO.attempt(HtmlDiff.unsafeOnlyBody(oldHtml, newHtml))
                              _       <- currentState.set(Some(newState))
                              _       <- domRef.set(Some(newHtml))
                              _       <- socket.sendBinary(DomChanged(domDiff).toProtocol)
                            yield ()
                          case _                          =>
                            for
                              newHtml <- render(newState, path)
                              _       <- currentState.set(Some(newState))
                              _       <- domRef.set(Some(newHtml))
                            yield ()
          yield ()
        }.runDrain.fork

      socketFib <-
        socket.receiveAll {
          case Read(WebSocketFrame.Binary(blob)) =>
            parseClientEvent(blob): clientEvent =>
              for
                oldState <- currentState.get
                oldHtml  <- domRef.get
                _        <- (oldState, oldHtml) match
                              case (Some(old), Some(oldHtml)) =>
                                for
                                  newState <- onEvent(old, clientEvent)
                                  newHtml  <- render(newState, path)
                                  domDiff  <- ZIO.attempt(HtmlDiff.unsafeOnlyBody(oldHtml, newHtml))
                                  _        <- currentState.set(Some(newState))
                                  _        <- domRef.set(Some(newHtml))
                                  _        <- socket.sendBinary(DomChanged(domDiff).toProtocol)
                                yield ()
                              case _                          =>
                                for
                                  _          <- logWarning("Got a message, but no state found")
                                  maybeState <- state.runHead
                                  _          <-
                                    maybeState.fold(logWarning("No state found")) { newState =>
                                      for
                                        newState <- onEvent(newState, clientEvent)
                                        newHtml  <- render(newState, path)
                                        _        <- currentState.set(Some(newState))
                                        _        <- domRef.set(Some(newHtml))
                                      yield ()
                                    }
                                yield ()
              yield ()
          case _                                 => logInfo("Got something else")
        }.fork

      _ <- ZIO.firstSuccessOf(socketFib.join, promise.await :: Nil)
      _ <- socketFib.join
    yield ()

/*
  final def run(channel: WebSocketChannel, path: Path): ZIO[Env, Throwable, Unit] = for
    _          <- logInfo("LiveView started.")
    maybeState <- state.runHead
    state      <- ZIO.fromOption(maybeState).orElseFail(new RuntimeException("No state found"))
    _          <- logInfo(s"State collected - ${maybeState}")

    stateObF <- state.tap { s =>
                  logInfo(s"New state --> ${s}")
                }.runDrain.fork
    stateRef <- Ref.make(maybeState)
    htmlRef  <- mount(path).flatMap(Ref.make(_))
    _        <-
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
    _        <- stateObF.join
  yield ()
 */
object LiveView:
  def route[Env, S](path: Path, view: LiveView[Env, S]): Routes[Env, Nothing] /* : Routes[Any, Nothing] */ =
    val (mainPath, wsPath) = path -> path / "ws"
    Routes(
      RoutePattern(GET, wsPath)   -> Handler.fromZIO(
        Handler.webSocket(channel => view.run(channel, wsPath)).toResponse
      ),
      RoutePattern(GET, mainPath) -> Handler.fromFunctionZIO(_ => view.mount(wsPath).map(Response.html(_)).orDie)
    )

  def routeZIO[Env, S](path: Path, viewZIO: ZIO[Env, Throwable, LiveView[Env, S]]): Routes[Env, Nothing] =
    val (mainPath, wsPath) = path -> path / "ws"
    Routes(
      RoutePattern(GET, wsPath)   -> Handler.fromZIO(
        viewZIO.flatMap { view =>
          Handler.webSocket(channel => view.run(channel, wsPath)).toResponse
        }.orDie
      ),
      RoutePattern(GET, mainPath) -> Handler.fromFunctionZIO(_ =>
        viewZIO.flatMap { view =>
          view.mount(wsPath).map(Response.html(_)).orDie
        }.orDie
      )
    )

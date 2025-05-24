package dev.lily.fe

import io.circe.parser.decode
import io.circe.generic.auto.*
import dev.lily.{ClientEvent, DomChanged}
import dev.lily.fe.DOMOps.*
import dev.lily.fe.JSONOps.*
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import org.scalajs.dom
import org.scalajs.dom.*
import dev.lily.DomChanged.domChangedDecoder

final private[fe] case class Lily private (
  private var statusBarUI: StatusBarUI,
  private val socket: WebSocket,
  private val body: HTMLElement,
  private val wsPath: String,
  private val reloadOnDisconnect: Boolean,
  private val reloadTimeout: Double
):
  private def load(event: dom.Event): Unit =
    statusBarUI.setLoaded()
    console.info(s"Lily activated. Connecting to \"$wsPath\"")
    connectToWS()
    attachEventHandlers()

  private def handleServerDOMChanged(event: DomChanged): Unit =
    HtmlDiffDOMPatcher.patchDom(body, event.diff)

  private def connectToWS(): Unit =
    socket.onmessage = (event: dom.MessageEvent) =>
      statusBarUI.incrementEventsReceived()
      for
        data            <- Option(event.data).flatMap(s => Option(s.toString))
        maybeDomChanged <- Option.when(data.contains("\"diff\""))(decode[DomChanged](data).toOption)
        _                = maybeDomChanged.foreach(handleServerDOMChanged)
      yield ()

    socket.onopen = (event: dom.Event) =>
      statusBarUI.setConnected()
      statusBarUI.incrementEventSent()

    socket.onclose = (event: dom.CloseEvent) =>
      statusBarUI.setDisconnected()
      if reloadOnDisconnect then console.info("Disconnected from Lily. Will reload in a few seconds.")
      window.setTimeout(
        () => if reloadOnDisconnect then window.location.reload(),
        reloadTimeout
      )

    socket.onerror = (event: dom.Event) => console.error(s"Error connecting to Lily w/ ${event}")

  private def attachEventHandlers(): Unit =
    document.addEventListener("click", localEventHandler(_))
    document.addEventListener("input", localEventHandler(_))
    document.addEventListener("mouseover", localEventHandler(_))
    document.addEventListener("mouseout", localEventHandler(_))

  private def localEventHandler(e: dom.Event): Unit =
    var (eventType, target) = e.`type` -> e.target.asInstanceOf[HTMLElement]

    while target != document do
      if target == null || target.parentElement == null then return

      target.dataStartsWith("data-li-on-").foreach { case (_, (rawDataKey, value)) =>
        val (clientEventName, serverEventName) = rawDataKey.stripPrefix("data-li-on-") -> value
        if clientEventName == eventType then
          val maybeValue: Option[String] = Option(target.asInstanceOf[HTMLInputElement].value)
          emitClientEvent(e, serverEventName, maybeValue)
      }

      if target == null || target.parentElement == null then return
      target = target.parentElement

  private def emitClientEvent(clientEvent: dom.Event, serverEventName: String, value: Option[String] = None): Unit =
    var (clientEventName, target) = clientEvent.`type` -> clientEvent.target.asInstanceOf[HTMLElement]
    val liID                      = clientEvent.target.asInstanceOf[HTMLElement].data("idli")
    socket.send(
      ClientEvent(
        clientEventName = clientEventName,
        serverEventName = serverEventName,
        liID = liID,
        value = value
      ).toProtocol
    )
    statusBarUI.incrementEventSent()

private[fe] object Lily:
  def load(statusBarUI: StatusBarUI, body: HTMLElement, event: dom.Event): Unit = for
    wsPath             <- body.data("li-ws-path")
    reloadOnDisconnect <-
      body
        .data("li-reload-on-disconnect")
        .flatMap(p => Option.when(p.contains("true"))(true))
        .orElse(Some(false))
    reloadTimeout      <- body.data("li-reload-timeout").flatMap(_.toDoubleOption).orElse(Some(2000d))
    webSocket           = new dom.WebSocket(wsPath)
  yield Lily(statusBarUI, webSocket, body, wsPath, reloadOnDisconnect, reloadTimeout).load(event)

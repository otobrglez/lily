package dev.lily.fe

import io.circe.parser.decode
import io.circe.generic.auto.*
import dev.lily.{ClientEvent, DomChanged, JavaScriptEvent}
import dev.lily.fe.DOMOps.*
import dev.lily.fe.JSONOps.*
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import org.scalajs.dom
import org.scalajs.dom.*
import dev.lily.DomChanged.domChangedDecoder

import scala.collection.mutable.ArrayBuffer as AB
import scala.scalajs.js.typedarray.*
import scala.scalajs.js.typedarray.Int8Array
import scala.scalajs.js
import scala.scalajs.js.JSON

final private[fe] case class Lily private (
  private var statusBarUI: StatusBarUI,
  private val socket: WebSocket,
  private val body: HTMLElement,
  private val wsPath: String,
  private val reloadOnDisconnect: Boolean,
  private val reloadTimeout: Double
):
  private val domChangesBuffer        = AB.empty[DomChanged]
  private var animationFrameRequested = false

  private def load(event: dom.Event): Unit =
    statusBarUI.setLoaded()
    console.info(s"Lily activated. Connecting to \"$wsPath\"")
    connectToWS()
    attachEventHandlers()

  private def processDomChangesBuffer(): Unit =
    domChangesBuffer.foreach(event => HtmlDiffDOMPatcher.patchDom(body, event.diff))
    domChangesBuffer.clear()
    animationFrameRequested = false

  private def handleServerDOMChanged(event: DomChanged): Unit =
    domChangesBuffer.append(event)
    if !animationFrameRequested then
      animationFrameRequested = true
      window.requestAnimationFrame(_ => processDomChangesBuffer())

  private def connectToWS(): Unit =
    socket.binaryType = "arraybuffer"
    socket.onmessage = (event: dom.MessageEvent) =>
      statusBarUI.incrementEventsReceived()
      for
        buffer     <- Option(event.data.asInstanceOf[ArrayBuffer]).map(b => new Int8Array(b).toArray)
        domChanged <- Option.when(buffer.length > 0)(DomChanged.fromBinary(buffer).toOption)
        _           = domChanged.foreach(handleServerDOMChanged)
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
    JavaScriptEvent.values.foreach { event =>
      document.addEventListener(event.name, localEventHandler(_))
    }

  private def getData(e: HTMLElement): List[String] = (for
    rawData <- Option(e.data("li-attached-data"))
    pom     <- rawData
                 .map(JSONSerde.singleQuotesToDoubleQuotes)
                 .flatMap(rIn => Option(JSON.parse(rIn)))
    values   = pom.asInstanceOf[js.Array[String]]
  yield values.toList).getOrElse(List.empty)

  private def localEventHandler(e: dom.Event): Unit =
    var (eventType, target) = e.`type` -> e.target.asInstanceOf[HTMLElement]

    while target != document do
      if target == null || target.parentElement == null then return

      target.dataStartsWith("data-li-on-").foreach { case (_, (rawDataKey, value)) =>
        val (clientEventName, serverEventName) = rawDataKey.stripPrefix("data-li-on-") -> value
        if clientEventName == eventType then
          emitClientEvent(e, serverEventName, Option(target.asInstanceOf[HTMLInputElement].value), getData(target))
      }

      if target == null || target.parentElement == null then return
      target = target.parentElement

  private def emitClientEvent(
    clientEvent: dom.Event,
    serverEventName: String,
    value: Option[String] = None,
    data: List[String] = List.empty
  ): Unit =
    var (clientEventName, target) = clientEvent.`type` -> clientEvent.target.asInstanceOf[HTMLElement]
    val liID                      = clientEvent.target.asInstanceOf[HTMLElement].data("idli")
    val buffer: ArrayBuffer       = ClientEvent(
      serverEventName = serverEventName,
      clientEventName = clientEventName,
      liID = liID,
      value = value,
      data = data
    ).toProtocol.toTypedArray.buffer
    socket.send(buffer)
    statusBarUI.incrementEventSent()

private[fe] object Lily:
  def load(statusBarUI: StatusBarUI, body: HTMLElement, event: dom.Event): Unit = for
    wsPath             <- body.data("li-ws-path")
    _                   = console.log("ws path ", wsPath)
    reloadOnDisconnect <-
      body
        .data("li-reload-on-disconnect")
        .flatMap(p => Option.when(p.contains("true") && wsPath.nonEmpty)(true))
        .orElse(Some(false))
    reloadTimeout      <- body.data("li-reload-timeout").flatMap(_.toDoubleOption).orElse(Some(2000d))
    webSocket           = new dom.WebSocket(wsPath)
  yield Lily(statusBarUI, webSocket, body, wsPath, reloadOnDisconnect, reloadTimeout).load(event)

package dev.lily.fe

import dev.lily.lhtml.{Html, HtmlRender}
import dev.lily.lhtml.syntax.{*, given}
import org.scalajs.dom.{console, document}

import javax.xml.transform.Templates

// Statusbar
final case class StatusBar(
  loaded: Boolean,
  connected: Boolean,
  eventSent: Int = 0,
  eventReceived: Int = 0
)

final case class StatusBarUI private (
  private var statusBar: StatusBar = StatusBar(false, false)
):
  private val spanPadding = "style" -> "padding: 5px;"
  private def render(): Html = div(
    span("🌸 Lily").withAttr(spanPadding),
    span(s"Loaded: ${statusBar.loaded}").withAttr(spanPadding),
    span(s"Connected: ${statusBar.connected}").withAttr(spanPadding),
    span(s"Events sent: ${statusBar.eventSent} ⬆").withAttr(spanPadding),
    span(s"Events received: ${statusBar.eventReceived} ⬇").withAttr(spanPadding)
  ).withAttr("style" ->
    """
      |position: fixed; bottom: 10px; right: 10px; display: inline-block; z-index: 999; font-size: 9pt;
      |padding: 5px; background-color: #000000; color: #ffffff; border-radius: 5px; font-family:monospace;
      |filter: drop-shadow(2px 2px 1px #999);""".stripMargin)

  private def upsertDOM(id: String = "lily-statusbar"): Unit =
    val statusBarElement = document.querySelector(s"div#$id")
    if statusBarElement == null then
      val statusBarDiv = document.createElement("div")
      statusBarDiv.id = id
      statusBarDiv.innerHTML = HtmlRender.render(render())
      document.body.appendChild(statusBarDiv)
    else statusBarElement.innerHTML = HtmlRender.render(render())

  private def updateRender(action: StatusBar => StatusBar): Unit =
    statusBar = action(statusBar)
    upsertDOM()

  def setLoaded(): Unit               = updateRender(_.copy(loaded = true))
  def setConnected(): Unit            = updateRender(_.copy(connected = true))
  def setDisconnected(): Unit         = updateRender(_.copy(connected = false))
  def incrementEventSent(): Unit      = updateRender(_.copy(eventSent = statusBar.eventSent + 1))
  def incrementEventsReceived(): Unit = updateRender(_.copy(eventReceived = statusBar.eventReceived + 1))

object StatusBarUI:
  def makeAttach(statusBar: StatusBar): StatusBarUI = StatusBarUI(statusBar)

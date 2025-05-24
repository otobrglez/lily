package dev.lily.fe

import org.scalajs.dom
import org.scalajs.dom.document

import java.time.LocalDateTime

val utc         = java.time.ZoneOffset.UTC
val statusBarUI = StatusBarUI.makeAttach(StatusBar(loaded = true, connected = true))

@main def main(): Unit =
  document.addEventListener("DOMContentLoaded", Lily.load(statusBarUI, document.body, _))

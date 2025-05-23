package dev.lily.fe

import org.scalajs.dom
import org.scalajs.dom.document

import java.time.LocalDateTime

val utc = java.time.ZoneOffset.UTC

@main def main(): Unit =
  document.addEventListener(
    "DOMContentLoaded",
    (event: dom.Event) =>
      val element = document.createElement("p")
      element.classList.add("clock")
      element.classList.add("frontend")
      element.innerHTML = s"JavaScript clock is ${LocalDateTime.now(utc)}"
      document.body.append(element)
  )

  document.addEventListener("DOMContentLoaded", Lily.load(document.body, _))

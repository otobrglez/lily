package dev.lilly.fe

import dev.lilly.HTMLZ
import org.scalajs.dom
import org.scalajs.dom.{console, document}
import java.time.LocalDateTime

val utc = java.time.ZoneOffset.UTC

@main def main(): Unit =
  console.log("Runtime is ready.")
  println(HTMLZ.sayHello("Oto Brglez"))
  println(s"Current client time is ${LocalDateTime.now(utc)}")

  document.addEventListener(
    "DOMContentLoaded",
    (event: dom.Event) =>
      console.log("DOM is ready.")
      val element = document.createElement("p")
      element.innerHTML = s"It is a new element and client time is ${LocalDateTime.now(utc)}"
      document.body.append(element)
  )

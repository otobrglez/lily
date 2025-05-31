package dev.lily.fe

import org.scalajs.dom.{Attr, Event, HTMLElement, MouseEvent}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.Object
import scala.scalajs.js.Dynamic.global

object DOMOps:

  def toJsonSafe(obj: js.Object) =
    val entries = js.Object.entries(obj)

    // Filter out properties that can't be serialized to JSON
    val validEntries = entries.filter { entry =>
      val value     = entry(1)
      val valueType = js.typeOf(value)

      // Keep only JSON-serializable types
      valueType != "function" &&
      valueType != "undefined" &&
      value != null &&
      !js.isUndefined(value)
    }

    js.Object.fromEntries(validEntries)

  extension (element: HTMLElement)
    def attr(key: String): Option[String] = for e <- Option(element.getAttribute(key))
    yield e

    def data(key: String): Option[String] =
      val withDataPrefix    = Option(element.getAttribute(s"data-$key"))
      val withoutDataPrefix = Option(element.getAttribute(key))
      withDataPrefix.orElse(withoutDataPrefix)

    def dataStartsWith(prefix: String): Option[(String, (String, String))] = for
      element         <- Option(element)
      attrs           <- Option(element.attributes).flatMap(a => Option(a.toVector))
      (dataKey, attr) <- attrs.find((k, _) => k.startsWith(prefix))
    yield (dataKey, (attr.name, attr.value))

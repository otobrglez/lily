package dev.lily

import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import zio.ZIO
import zio.http.template.Html as ZIOHtml

object HTMLOps:
  extension (h: Html)
    private def asZIOHtml: ZIOHtml = ZIOHtml.raw(Html.renderWithIndent(h))

    def on(clientEvent: String, serverEvent: String): Html =
      h.data("li-on-" + clientEvent, serverEvent)

    def on(tpl: (String, String)): Html = on(tpl._1, tpl._2)

    private def escapeJsonString(s: String): String =
      s.replace("\\", "\\\\")  // Escape backslashes first (must be first)
        .replace("\"", "\\\"") // Escape double quotes
        .replace("'", "\\'")   // Escape single quotes (for JS compatibility)
        .replace("\b", "\\b")  // Escape backspace
        .replace("\f", "\\f")  // Escape form feed
        .replace("\n", "\\n")  // Escape newline
        .replace("\r", "\\r")  // Escape carriage return
        .replace("\t", "\\t")  // Escape tab
        .replace("/", "\\/") // Escape forward slash (optional but recommended)

    private def stringListToJsonArray(strings: List[String], useSingleQuotes: Boolean = false): String =
      val quoteChar     = if useSingleQuotes then "'" else "\""
      val escapedValues = strings.map(s => s"$quoteChar${escapeJsonString(s)}$quoteChar")

      "[" + escapedValues.mkString(",") + "]"

    def on(clientEvent: String, serverEvent: String, attachedData: List[String] = List.empty): Html =
      if attachedData.isEmpty then on(clientEvent, serverEvent)
      else
        on(clientEvent, serverEvent).data(
          "li-attached-data",
          stringListToJsonArray(attachedData, useSingleQuotes = true)
        )

  given Conversion[Html, ZIOHtml] = asZIOHtml

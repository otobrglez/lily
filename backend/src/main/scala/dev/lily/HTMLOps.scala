package dev.lily

import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import zio.ZIO
import zio.http.template.Html as ZIOHtml

object HTMLOps:
  extension (h: Html)
    private def asZIOHtml: ZIOHtml = ZIOHtml.raw(Html.renderWithIndent(h))

    def on(clientEvent: String, serverEvent: String): Html =
      h.withData("li-on-" + clientEvent, serverEvent)

  given Conversion[Html, ZIOHtml] = asZIOHtml

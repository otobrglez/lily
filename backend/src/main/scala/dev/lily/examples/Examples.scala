package dev.lily.examples

import dev.lily.HTMLOps.{*, given}
import dev.lily.LiveHtml
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import zio.http.Path

// This is just a layout to make things easier on the eyes.
object Examples extends LiveHtml:
  private val mainCSS =
    """
      |html, body, input, label, td, th, p, ul, li { font-family: sans-serif; font-size: 12pt; }
      |body { padding: 10px; margin: 0; }
      |table td, table th { padding: 5px; }
      |table thead th { text-align: center; }
      |table input[type=number], table td, table th { text-align: right; }
      |table input[type=number] { width: 80px; }
      |table tbody tr:nth-child(even) td { background-color: #EEE; }
      |table { border-collapse: collapse; }
      |table tr td { border-top: 1px solid #EEE; }
      |div.content { margin: 0 auto; max-width: 1200px; padding: 20px }
      |.tools button { margin-right: 10px; } """.stripMargin

  def layout(pageTitle: Option[String], path: Option[Path] = Some(Path.empty), moreCss: Option[String] = None)(
    content: Html*
  ): Html =
    html(
      head(
        title(pageTitle.fold("Lily")(t => s"$t - Lily")),
        meta().attr("charset" -> "utf-8"),
        meta().attr("viewport" -> "width=device-width, initial-scale=1"),
        style(moreCss.fold(mainCSS)(css => mainCSS + "\n" + css))
      ),
      bodyOn(path)(
        div(
          menu(path),
          div(
            content
          ).klass("content-inner")
        ).klass("content")
      )
    )

  private def menu(path: Option[Path] = Some(Path.empty)): Html = path
    .filterNot(_ == Path.empty)
    .fold(
      div().attr("style" -> "display: none")
    )(path =>
      div(
        p(a("Back to examples.").attr("href" -> "/"))
      ).klass("menu")
    )

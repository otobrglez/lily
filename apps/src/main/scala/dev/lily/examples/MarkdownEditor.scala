package dev.lily.examples

import dev.lily.ClientEvent.on
import dev.lily.HTMLOps.{*, given}
import dev.lily.lhtml.{Html, HtmlIdEnhancer}
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.{ClientEvent, LiveView}
import zio.ZIO.{logError, logInfo}
import zio.http.Path
import zio.stream.ZStream
import zio.{Ref, Task, ZIO}
import dev.lily.ClientEvent.{on, onData}
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

import scala.util.Random.javaRandomToRandom

type Markdown = String

object Markdown:
  def render(raw: String): Task[Markdown] = for
    parser   <- ZIO.attempt(Parser.builder().build())
    node     <- ZIO.attempt(parser.parse(raw))
    renderer <- ZIO.attempt(HtmlRenderer.builder().build())
  yield renderer.render(node)

type OutputHtml = Html
object MarkdownEditor:
  def make = for
    mdRef     <-
      Ref.make(
        """# Hello, world!
          |
          |This is a markdown editor.
          |It renders [markdown](https://en.wikipedia.org/wiki/Markdown) to HTML and allows you to edit it.
          |
          |Rendering is done on the server as you type.
          |
          |Try it out!
          |
          |\- Oto""".stripMargin
      )
    outputRef <- mdRef.get.flatMap(Markdown.render).map(HtmlFromJsoup.fromString).flatMap(Ref.make)
  yield new MarkdownEditor(mdRef, outputRef)

final case class MarkdownEditor(
  private val md: Ref[Markdown],
  private val output: Ref[OutputHtml]
) extends LiveView[Any, (Markdown, OutputHtml)]:
  def initialState = ZStream.fromZIO(md.get <*> output.get)

  def on(s: (Markdown, OutputHtml)): Handler =
    case on("set", None)        => ZIO.succeed("" -> "")
    case on("set", Some(value)) =>
      val cleanValue = value.strip().trim
      for
        _        <- md.set(cleanValue)
        markdown <- Markdown.render(cleanValue)
        html     <- ZIO
                      .succeed(markdown)
                      .flatMap(r => ZIO.attempt(HtmlFromJsoup.fromString(r)))
                      .catchAll(th => ZIO.succeed(div("Error rendering markdown: " + th.getMessage)))
        newH     <- output.updateAndGet(_ => html)
      yield cleanValue -> html

  private val css: String =
    """
      |.editor {
      | width: 100%;
      | height: 10vh;
      | display: flex;
      | flex-direction: column;
      | box-sizing: border-box;
      |}
      |.editor-inner {
      |  display: flex;
      |  flex-direction: row;
      |}
      |.editor-left,
      |.editor-right {
      |  width: 50%;
      |  height: 100%;
      |  box-sizing: border-box;
      |  padding: 0.45rem;
      |  overflow: auto;
      |}
      |.editor-input {
      |  width: 100%;
      |  height: 100%;
      |  min-height: 300px;
      |  font-size: 1rem;
      |  font-family: monospace;
      |  border: 1px solid #e3e3e3;
      |  border-radius: 4px;
      |  padding: 0.75rem;
      |  background: #f9f9fa;
      |  resize: none;
      |  box-sizing: border-box;
      |}
      |
      |""".stripMargin

  private val random = scala.util.Random

  def render(state: (Markdown, OutputHtml), path: Path): Task[Html] = ZIO.succeed:
    val (markdown, html) = state
    Examples.layout(Some("Markdown Editor"), Some(path), Some(css))(
      h1("Lily - Markdown Editor"),
      div(
        div(
          div(
            textarea(markdown)
              .attr("autofocus" -> "true")
              .attr("placeholder" -> "Type your markdown here...")
              .klass("editor-input")
              .on("input" -> "set")
          ).klass("editor-left"),
          div(
            div(html).klass("preview-content")
          ).klass("editor-right").forceReplace
        ).klass("editor-inner")
      ).klass("editor")
    )

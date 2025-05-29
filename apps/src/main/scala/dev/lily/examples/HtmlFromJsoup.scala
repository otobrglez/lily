package dev.lily.examples

import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import org.jsoup.nodes.{Element as JsoupElement, Node, TextNode}
import scala.jdk.CollectionConverters.*

object HtmlFromJsoup:
  def fromString(html: String): Html =
    val document = org.jsoup.Jsoup.parseBodyFragment(html)
    val body     = document.body()
    val children = body.childNodes().asScala.map(nodeToHtml).toList
    children match
      case Nil      => Html.text("")
      case h :: Nil => h
      case many     => Html.div(many*)

  private def nodeToHtml(node: Node): Html =
    node match
      case e: JsoupElement =>
        val attrs    = e.attributes().asList().asScala.map(attr => attr.getKey -> attr.getValue).toMap
        val children = e.childNodes().asScala.map(nodeToHtml).toList
        Html.element(e.tagName, attrs, children*)
      case t: TextNode     =>
        Html.text(t.text())
      case _               =>
        // Html.element("div", Map.empty)
        Html.text("") // Fallback for comments, etc.

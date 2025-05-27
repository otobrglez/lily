package dev.lily.lhtml

import scala.annotation.targetName

type Attributes = Map[String, String]
object Attributes:
  def empty: Attributes = Map.empty

sealed trait HtmlF[+A]
final case class Element[A](
  tag: String,
  attributes: Attributes = Attributes.empty,
  children: List[A]
) extends HtmlF[A]
final case class Text(content: String) extends HtmlF[Nothing]

final case class Fix[F[_]](unfix: F[Fix[F]])
type Html = Fix[HtmlF]

trait Functor[F[_]]:
  def map[A, B](fa: F[A])(f: A => B): F[B]

given Functor[HtmlF] with
  def map[A, B](fa: HtmlF[A])(f: A => B): HtmlF[B] = fa match
    case Element(tag, attributes, children) => Element(tag, attributes, children.map(f))
    case Text(content)                      => Text(content)

private[lhtml] def cata[F[_]: Functor, A](term: Fix[F])(algebra: F[A] => A): A =
  val F = summon[Functor[F]]
  algebra(F.map(term.unfix)(cata(_)(algebra)))

object HtmlAttributes:
  extension (html: Html)
    def id(id: String): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs + ("id" -> id), children))
      case other                              => other

    def klass(className: String): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs + ("class" -> className), children))
      case other                              => other

    def style(className: String): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs + ("style" -> className), children))
      case other                              => other

    def attr(name: String, value: String): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs + (name -> value), children))
      case other                              => other

    def attrs(attributes: Attributes): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs ++ attributes, children))
      case other                              => other

    def attr(attributes: (String, String)*): Html = attrs(attributes.toMap)

    def data(name: String, value: String): Html   = attr("data-" + name, value)
    def data(attributes: (String, String)*): Html = attrs(attributes.map(kv => "data-" + kv._1 -> kv._2).toMap)

    infix def ++(other: Html): Seq[Html] = Seq(html, other)

  extension (htmls: Seq[Html])
    infix def ++(other: Html): Seq[Html]       = htmls :+ other
    infix def ++(others: Seq[Html]): Seq[Html] = htmls ++ others

object Html:
  def text(content: String): Html = Fix(Text(content))
  given Conversion[String, Html]  = s => Html.text(s)

  def element(tag: String): Html                                        = element(tag, Map.empty)
  def element(tag: String, children: Html*): Html                       = element(tag, Map.empty, children*)
  def element(t: String, attributes: Attributes, children: Html*): Html = Fix(Element(t, attributes, children.toList))

  def body(attributes: Attributes, children: Html*): Html      = element("body", attributes, children*)
  def body(children: Html*): Html                              = element("body", children*)
  @targetName("bodySeq") def body(children: Seq[Html]): Html   = element("body", children*)
  def head(attributes: Attributes, children: Html*): Html      = element("head", attributes, children*)
  def head(children: Html*): Html                              = element("head", children*)
  def button(attributes: Attributes, children: Html*): Html    = element("button", attributes, children*)
  def button(children: Html*): Html                            = element("button", children*)
  def textarea(children: Html*): Html                          = element("textarea", children*)
  def textarea(attributes: Attributes): Html                   = element("textarea", attributes)
  def div(attributes: Attributes, children: Html*): Html       = element("div", attributes, children*)
  def div(children: Html*): Html                               = element("div", children*)
  @targetName("divSeq") def div(children: Seq[Html]): Html     = element("div", children*)
  def table(attributes: Attributes, children: Html*): Html     = element("table", attributes, children*)
  def table(children: Html*): Html                             = element("table", children*)
  @targetName("tableSeq") def table(children: Seq[Html]): Html = element("table", children*)
  def tr(attributes: Attributes, children: Html*): Html        = element("tr", attributes, children*)
  def tr(children: Html*): Html                                = element("tr", children*)
  @targetName("trSeq") def tr(children: Seq[Html]): Html       = element("tr", children*)
  def thead(attributes: Attributes, children: Html*): Html     = element("thead", attributes, children*)
  def thead(children: Html*): Html                             = element("thead", children*)
  @targetName("theadSeq") def thead(children: Seq[Html]): Html = element("thead", children*)
  def tbody(attributes: Attributes, children: Html*): Html     = element("tbody", attributes, children*)
  def tbody(children: Html*): Html                             = element("tbody", children*)
  @targetName("tbodySeq") def tbody(children: Seq[Html]): Html = element("tbody", children*)
  def tfoot(attributes: Attributes, children: Html*): Html     = element("tfoot", attributes, children*)
  def tfoot(children: Html*): Html                             = element("tfoot", children*)
  @targetName("tfootSeq") def tfoot(children: Seq[Html]): Html = element("tfoot", children*)
  def caption(attributes: Attributes, children: Html*): Html   = element("caption", attributes, children*)
  def caption(children: Html*): Html                           = element("caption", children*)
  def td(attributes: Attributes, children: Html*): Html        = element("td", attributes, children*)
  @targetName("tdSeq") def td(children: Seq[Html]): Html       = element("td", children*)
  def td(children: Html*): Html                                = element("td", children*)
  def th(attributes: Attributes, children: Html*): Html        = element("th", attributes, children*)
  def th(children: Html*): Html                                = element("th", children*)
  @targetName("thSeq") def th(children: Seq[Html]): Html       = element("th", children*)
  def form(attributes: Attributes, children: Html*): Html      = element("form", attributes, children*)
  def form(children: Html*): Html                              = element("form", children*)
  def h1(attributes: Attributes, children: Html*): Html        = element("h1", attributes, children*)
  def h1(children: Html*): Html                                = element("h1", children*)
  def h2(attributes: Attributes, children: Html*): Html        = element("h2", attributes, children*)
  def h2(children: Html*): Html                                = element("h2", children*)
  def h3(attributes: Attributes, children: Html*): Html        = element("h3", attributes, children*)
  def h3(children: Html*): Html                                = element("h3", children*)
  def html(children: Html*): Html                              = element("html", children*)
  @targetName("htmlSeq") def html(children: Seq[Html]): Html   = element("html", children*)
  def input(attributes: Attributes, children: Html*): Html     = element("input", attributes, children*)
  def input(children: Html*): Html                             = element("input", children*)
  def li(children: Html*): Html                                = element("li", children*)
  def li(attributes: Attributes, children: Html*): Html        = element("li", attributes, children*)
  @targetName("liSeq") def li(children: Seq[Html]): Html       = element("li", children*)
  def p(attributes: Attributes, children: Html*): Html         = element("p", attributes, children*)
  def p(children: Html*): Html                                 = element("p", children*)
  @targetName("pSeq") def p(children: Seq[Html]): Html         = element("p", children*)
  def span(attributes: Attributes, children: Html*): Html      = element("span", attributes, children*)
  def span(children: Html*): Html                              = element("span", children*)
  def a(attributes: Attributes, children: Html*): Html         = element("a", attributes, children*)
  def a(children: Html*): Html                                 = element("a", children*)
  @targetName("aSeq") def a(children: Seq[Html]): Html         = element("a", children*)
  def title(children: Html*): Html                             = element("title", children*)
  def title(attributes: Attributes, children: Html*): Html     = element("title", children*)

  def style(children: Html*): Html                         = element("style", children*)
  def style(attributes: Attributes, children: Html*): Html = element("style", children*)

  def ul(attributes: Attributes, children: Html*): Html     = element("ul", attributes, children*)
  def ul(children: Html*): Html                             = element("ul", children*)
  @targetName("ulSeq") def ul(children: Seq[Html]): Html    = element("ul", children*)
  def meta(attributes: Attributes, children: Html*): Html   = element("meta", attributes, children*)
  def meta(children: Html*): Html                           = element("meta", children*)
  def script(attributes: Attributes, children: Html*): Html = element("script", attributes, children*)
  def script(children: Html*): Html                         = element("script", children*)

  def renderWithIndent(html: Html, indentSize: Int = 2): String      = HtmlRender.render(html, indentSize)
  def renderToJson(html: Html, prettyPrint: Boolean = false): String = HtmlJsonRenderer.renderToJson(html, prettyPrint)

  def attachLIIDs(html: Html, start: Int = 0): Html = HtmlIdEnhancer.addIds(html, start)

object syntax:
  // import Html.{*, given}
  export Html.{*, given}
  export HtmlAttributes.{++, attr, attrs, data, id, klass, style}

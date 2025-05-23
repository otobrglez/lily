package dev.lily.lhtml

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
    def withId(id: String): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs + ("id" -> id), children))
      case other                              => other

    def withClass(className: String): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs + ("class" -> className), children))
      case other                              => other

    def withAttr(name: String, value: String): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs + (name -> value), children))
      case other                              => other

    def withAttrs(attributes: Attributes): Html = html match
      case Fix(Element(tag, attrs, children)) => Fix(Element(tag, attrs ++ attributes, children))
      case other                              => other

    def withAttr(attributes: (String, String)*): Html = withAttrs(attributes.toMap)

    def withData(name: String, value: String): Html   = withAttr("data-" + name, value)
    def withData(attributes: (String, String)*): Html = withAttrs(attributes.map(kv => "data-" + kv._1 -> kv._2).toMap)

object Html:
  def text(content: String): Html = Fix(Text(content))
  given Conversion[String, Html]  = s => Html.text(s)

  def element(tag: String): Html                                        = element(tag, Map.empty)
  def element(tag: String, children: Html*): Html                       = element(tag, Map.empty, children*)
  def element(t: String, attributes: Attributes, children: Html*): Html = Fix(Element(t, attributes, children.toList))

  def body(attributes: Attributes, children: Html*): Html   = element("body", attributes, children*)
  def body(children: Html*): Html                           = element("body", children*)
  def head(attributes: Attributes, children: Html*): Html   = element("head", attributes, children*)
  def head(children: Html*): Html                           = element("head", children*)
  def button(attributes: Attributes, children: Html*): Html = element("button", attributes, children*)
  def button(children: Html*): Html                         = element("button", children*)
  def div(attributes: Attributes, children: Html*): Html    = element("div", attributes, children*)
  def div(children: Html*): Html                            = element("div", children*)
  def form(attributes: Attributes, children: Html*): Html   = element("form", attributes, children*)
  def form(children: Html*): Html                           = element("form", children*)
  def h1(attributes: Attributes, children: Html*): Html     = element("h1", attributes, children*)
  def h1(children: Html*): Html                             = element("h1", children*)
  def h2(attributes: Attributes, children: Html*): Html     = element("h2", attributes, children*)
  def h2(children: Html*): Html                             = element("h2", children*)
  def h3(attributes: Attributes, children: Html*): Html     = element("h3", attributes, children*)
  def h3(children: Html*): Html                             = element("h3", children*)
  def html(children: Html*): Html                           = element("html", children*)
  def input(attributes: Attributes, children: Html*): Html  = element("input", attributes, children*)
  def input(children: Html*): Html                          = element("input", children*)
  def li(children: Html*): Html                             = element("li", children*)
  def li(attributes: Attributes, children: Html*): Html     = element("li", attributes, children*)
  def p(attributes: Attributes, children: Html*): Html      = element("p", attributes, children*)
  def p(children: Html*): Html                              = element("p", children*)
  def span(attributes: Attributes, children: Html*): Html   = element("span", attributes, children*)
  def span(children: Html*): Html                           = element("span", children*)
  def a(attributes: Attributes, children: Html*): Html      = element("a", attributes, children*)
  def a(children: Html*): Html                              = element("a", children*)
  def title(children: Html*): Html                          = element("title", children*)
  def title(attributes: Attributes, children: Html*): Html  = element("title", children*)
  def ul(attributes: Attributes, children: Html*): Html     = element("ul", attributes, children*)
  def ul(children: Html*): Html                             = element("ul", children*)
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
  export HtmlAttributes.{withAttr, withAttrs, withClass, withData, withId}

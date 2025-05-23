package dev.lily.lhtml

object HtmlRender:
  final private case class Config(
    indentSize: Int = 2,
    currentLevel: Int = 0
  )

  private def renderHtml(config: Config): HtmlF[String] => String =
    case Element(tag, attributes, children) if attributes.isEmpty => s"<$tag>${children.mkString}</$tag>"
    case Element(tag, attributes, children)                       =>
      s"<$tag ${attributes.map { case (k, v) => s"$k=\"$v\"" }.mkString(" ")}>${children.mkString}</$tag>"
    case Text(content)                                            => content

  def render(html: Html, indentSize: Int = 2): String =
    cata(html)(renderHtml(Config(indentSize = indentSize)))

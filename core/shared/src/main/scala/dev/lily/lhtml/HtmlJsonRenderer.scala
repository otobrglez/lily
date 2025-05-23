package dev.lily.lhtml

object HtmlJsonRenderer:

  def renderToJson(html: Html, prettyPrint: Boolean = false): String =
    val jsonTree = cata(html)(htmlToJsonObject)
    if prettyPrint then prettyPrintJson(jsonTree, 0)
    else jsonTree

  private def htmlToJsonObject: HtmlF[String] => String =
    case Element(tag, attributes, children) =>
      val attributesJson =
        if attributes.isEmpty then "\"attributes\": {}"
        else
          s"""\"attributes\": {${attributes.map { case (k, v) =>
              s"\"${escapeJsonString(k)}\": \"${escapeJsonString(v)}\""
            }.mkString(", ")}}"""

      val childrenJson =
        if children.isEmpty then "\"children\": []"
        else s"\"children\": [${children.mkString(", ")}]"

      s"""{\"type\": \"element\", \"tag\": \"$tag\", $attributesJson, $childrenJson}"""

    case Text(content) =>
      s"""{\"type\": \"text\", \"content\": \"${escapeJsonString(content)}\"}"""

  private def escapeJsonString(s: String): String =
    s.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")

  private def prettyPrintJson(json: String, level: Int): String =
    val indentSize = 2
    val indent     = " " * (indentSize * level)
    val nextIndent = " " * (indentSize * (level + 1))

    json
      .replace("{", "{\n" + nextIndent)
      .replace("}", "\n" + indent + "}")
      .replace("[", "[\n" + nextIndent)
      .replace("]", "\n" + indent + "]")
      .replace(", \"", ",\n" + nextIndent + "\"")
      .replace(", {", ",\n" + nextIndent + "{")

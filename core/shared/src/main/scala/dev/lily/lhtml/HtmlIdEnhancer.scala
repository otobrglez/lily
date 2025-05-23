package dev.lily.lhtml

object HtmlIdEnhancer:
  private val excluded = Set(
    "body",
    "head",
    "html",
    "link",
    "meta",
    "script",
    "style",
    "title"
  )

  private def traverse(html: Html, nextId: Int): (Html, Int) =
    html match
      case Fix(Text(content))                 => (html, nextId)
      case Fix(Element(tag, attrs, children)) =>
        val thisId = nextId

        val (processedChildren, finalId) = children.foldLeft((List.empty[Html], thisId + 1)) {
          case ((processed, id), child) =>
            val (processedChild, nextId) = traverse(child, id)
            // Append.
            (processed :+ processedChild) -> nextId
        }

        val newElement =
          if excluded.contains(tag) then Fix(Element(tag, attrs, processedChildren))
          else Fix(Element(tag, attrs + ("data-idli" -> thisId.toString), processedChildren))
        end newElement

        newElement -> finalId

  def addIds(html: Html, start: Int = 0, attrPrefix: String = "data-idli"): Html =
    val (result, _) = traverse(html, start)
    result

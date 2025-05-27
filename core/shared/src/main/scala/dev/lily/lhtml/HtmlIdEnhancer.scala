package dev.lily.lhtml

final class HtmlIdEnhancer[N](
  idGen: N => N = identity[N],
  private val excludedTags: Set[String] = Set(
    "body",
    "head",
    "html",
    "link",
    "meta",
    "script",
    "style",
    "title"
  )
):

  private def traverse(html: Html, nextId: N): (Html, N) =
    html match
      case Fix(Text(content))                 => (html, nextId)
      case Fix(Element(tag, attrs, children)) =>
        val thisId            = nextId
        val nextIdForChildren = idGen(thisId)

        val (processedChildren, finalId) = children.foldLeft((List.empty[Html], nextIdForChildren)) {
          case ((processed, id), child) =>
            val (processedChild, nextId) = traverse(child, id)
            // Append.
            (processed :+ processedChild) -> nextId
        }

        val newElement =
          if excludedTags.contains(tag) then Fix(Element(tag, attrs, processedChildren))
          else Fix(Element(tag, attrs + ("data-idli" -> thisId.toString), processedChildren))
        end newElement

        newElement -> finalId

  def addIds(html: Html, start: N, attrPrefix: String = "data-idli"): Html =
    val (result, _) = traverse(html, start)
    result

object HtmlIdEnhancer:
  private def addIds[N](html: Html, start: N = ???)(idGen: N => N): Html =
    new HtmlIdEnhancer[N](idGen).addIds(html, start = start)

  def addNumericIDs(html: Html, start: Int = 0): Html = addIds(html, start = start)(_ + 1)

  def addRandomIDs(html: Html, stringSize: Int = 10): Html =
    val random = scala.util.Random
    addIds[String](html, start = random.nextString(stringSize))(_ => random.nextString(stringSize))

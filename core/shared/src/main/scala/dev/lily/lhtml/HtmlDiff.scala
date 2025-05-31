package dev.lily.lhtml

sealed trait HtmlDiff
object HtmlDiff:
  case object NoChange                                                                     extends HtmlDiff
  final case class Replace(newNode: Html)                                                  extends HtmlDiff
  final case class ChangeAttrs(attrs: List[AttrChanged], children: Option[ChangeChildren]) extends HtmlDiff
  final case class ChangeChildren(changes: List[(Int, HtmlDiff)])                          extends HtmlDiff
  final case class AttrChanged(key: String, from: Option[String], to: Option[String])      extends HtmlDiff

  def unsafeOnlyBody(a: Html, b: Html): HtmlDiff =
    val bodyA = a.unfix match
      case Element("html", _, List(_, body)) => body
      case Element("html", _, List(body))    => body
      case _                                 => throw new RuntimeException("Invalid root structure for A")
    val bodyB = b.unfix match
      case Element("html", _, List(_, body)) => body
      case Element("html", _, List(body))    => body
      case _                                 => throw new RuntimeException("Invalid root structure for B")

    HtmlDiff.diff(bodyA, bodyB)

  /*
  def diff(old: Html, current: Html): HtmlDiff = (old.unfix, current.unfix) match
    case (Text(t1), Text(t2)) =>
      if t1 == t2 then NoChange
      else Replace(current)

    case (Element(tag1, attrs1, children1), Element(tag2, attrs2, children2)) =>
      if tag1 != tag2 then Replace(current)
      else
        val attrDiffs  = diffAttrs(attrs1, attrs2)
        val childDiffs = diffChildren(children1, children2)

        attrDiffs -> childDiffs match
          case (Nil, Nil) => NoChange
          case (a, Nil)   => ChangeAttrs(a, None)
          case (Nil, c)   => ChangeChildren(c)
          case (a, c)     => ChangeAttrs(a, Some(ChangeChildren(c)))

    // Mismatched node types (Text vs Element, or other)
    case _ => Replace(current)
   */

  def diff(old: Html, current: Html): HtmlDiff = (old.unfix, current.unfix) match
    case (Text(t1), Text(t2)) =>
      if t1 == t2 then NoChange
      else Replace(current)

    case (Element(tag1, attrs1, children1), Element(tag2, attrs2, children2)) =>
      // Force Replace if 'data-force-replace' attribute changes
      val key1 = attrs1.get("data-force-replace")
      val key2 = attrs2.get("data-force-replace")
      if key1 != key2 then Replace(current)
      else if tag1 != tag2 then Replace(current)
      else
        val attrDiffs  = diffAttrs(attrs1, attrs2)
        val childDiffs = diffChildren(children1, children2)

        attrDiffs -> childDiffs match
          case (Nil, Nil) => NoChange
          case (a, Nil)   => ChangeAttrs(a, None)
          case (Nil, c)   => ChangeChildren(c)
          case (a, c)     => ChangeAttrs(a, Some(ChangeChildren(c)))

    // Mismatched node types (Text vs Element, or other)
    case _ => Replace(current)

  private def diffAttrs(a: Map[String, String], b: Map[String, String]): List[AttrChanged] =
    val keys = a.keySet union b.keySet
    keys.toList.flatMap { k =>
      val v1 = a.get(k)
      val v2 = b.get(k)
      if v1 != v2 then Some(AttrChanged(k, v1, v2)) else None
    }

  private def diffChildren(old: List[Html], current: List[Html]): List[(Int, HtmlDiff)] =
    val maxLen = math.max(old.length, current.length)

    (0 until maxLen).toList.flatMap { i =>
      (old.lift(i), current.lift(i)) match
        case (Some(o), Some(c)) =>
          val d = diff(o, c)
          if d == NoChange then None else Some(i -> d)

        case (None, Some(c)) =>
          Some(i -> Replace(c))

        case (Some(_), None) =>
          Some(i -> Replace(Fix(Text("")))) // or a special Remove?

        case _ => None
    }

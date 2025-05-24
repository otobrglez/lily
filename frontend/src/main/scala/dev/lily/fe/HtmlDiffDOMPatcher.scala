package dev.lily.fe

import org.scalajs.dom
import org.scalajs.dom.*

import dev.lily.lhtml.{Html, HtmlDiff}
import HtmlDiff.*
import dev.lily.lhtml

object HtmlDiffDOMPatcher:
  def patchDom(el: dom.Element, diff: HtmlDiff): Unit = diff match
    case NoChange => ()

    case Replace(newNode) =>
      val newEl = renderHtmlToDom(newNode)
      el.replaceWith(newEl)

    case ChangeAttrs(attrDiffs, maybeChildren) =>
      applyAttrDiffs(el, attrDiffs)
      maybeChildren.foreach(ch => patchChildren(el, ch))

    case ChangeChildren(changes) =>
      // Handle text nodes specially
      if el.childNodes.length == 1 && el.firstChild.nodeType == dom.Node.TEXT_NODE &&
        changes.length == 1 && changes.head._1 == 0
      then
        // Direct text content replacement for single text node
        changes.head._2 match
          case Replace(newNode) =>
            newNode.unfix match
              case lhtml.Text(content) =>
                el.textContent = content
              case _                   =>
                val newEl = renderHtmlToDom(newNode)
                el.innerHTML = ""
                el.appendChild(newEl)
          case _                =>
            patchChildren(el, ChangeChildren(changes))
      else
        // Normal child patching for multiple/complex children
        patchChildren(el, ChangeChildren(changes))

    case _ =>
      console.error("Something went wrong.")
      ()

  /** Apply attribute changes to a DOM element */
  private def applyAttrDiffs(el: dom.Element, diffs: List[AttrChanged]): Unit =
    diffs.foreach {
      case AttrChanged(k, _, Some(v)) => el.setAttribute(k, v)
      case AttrChanged(k, _, None)    => el.removeAttribute(k)
    }

  private def patchChildren(el: dom.Element, diff: ChangeChildren): Unit =
    // First, organize changes by index for easier access
    val changesByIndex = diff.changes.toMap

    // Loop through all indices that might need changes
    val maxIndex = if diff.changes.isEmpty then 0 else diff.changes.map(_._1).max

    // Process all child elements that need updates
    for i <- 0 to math.max(el.children.length - 1, maxIndex) do
      changesByIndex.get(i) match
        // When there's a change for this index
        case Some(subDiff) =>
          if i < el.children.length then
            // Element exists - either update or replace it
            val child = el.children(i)
            subDiff match
              case Replace(newNode) =>
                // Complete replacement - create new element and replace the old one
                val newEl = renderHtmlToDom(newNode)
                el.replaceChild(newEl, child)
              case _                =>
                // Apply other diff types recursively
                patchDom(child, subDiff)
          else
            // Element doesn't exist - append new element
            subDiff match
              case Replace(newNode) =>
                val newEl = renderHtmlToDom(newNode)
                el.appendChild(newEl)
              case _                => ()

        case None => ()

  private def renderHtmlToDom(html: Html): dom.Node = html.unfix match
    case lhtml.Text(content)                 => dom.document.createTextNode(content)
    case lhtml.Element(tag, attrs, children) =>
      val el = dom.document.createElement(tag)
      attrs.foreach { case (k, v) => el.setAttribute(k, v) }
      children.map(renderHtmlToDom).foreach(el.appendChild)
      el

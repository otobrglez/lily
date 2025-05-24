package dev.lily.lhtml

import dev.lily.lhtml.HtmlDiff.AttrChanged
import zio.test.*
import dev.lily.lhtml.syntax.{*, given}
import io.circe.generic.auto.*
import io.circe.syntax.*
import HtmlJsonCodecs.{*, given}
import io.circe.Json

object HtmlDiffSpec extends ZIOSpecDefault:
  def spec = suite("HtmlDiffSpec")(
    test("No change") {
      val a = html(
        head(title("Hello world")),
        body(
          div(div(h1("Hello world from Lily!"))),
          div(
            div(
              p("test"),
              h1("another"),
              h3("what")
            )
          )
        )
      )

      val diff = HtmlDiff.diff(a, a)
      assertTrue(diff == HtmlDiff.NoChange)
    },
    test("First level") {
      val a    = div(div("Hello."))
      val b    = div(div("Hello World."))
      val diff = HtmlDiff.diff(a, b)
      assertTrue(diff.isInstanceOf[HtmlDiff.ChangeChildren])
    },
    test("Attribute change") {
      val a    = div("Hello.").withAttr("data-test", "false")
      val b    = div("Hello.").withAttr("data-test", "true")
      val diff = HtmlDiff.diff(a, b)
      assertTrue(diff.isInstanceOf[HtmlDiff.ChangeAttrs])
    },
    test("Order change") {
      val a    = div("Hello.", div(div("a"), div("b")))
      val b    = div("Hello.", div(div("b"), div("a")))
      val diff = HtmlDiff.diff(a, b)
      assertTrue(diff.isInstanceOf[HtmlDiff.ChangeChildren])
    },
    test("HTML and DIFF to JSON and back") {
      val a    = div("Hello.", div(div("a"), div("b")))
      val b    = div("Hello.", div(div("b"), div("a"))).withAttr("data-test", "true")
      val diff = HtmlDiff.diff(a, b)

      val aJson          = encodeHtml(a)
      val aDecoded: Html =
        decodeHtml
          .decodeJson(io.circe.parser.parse(aJson.toString).getOrElse(Json.Null))
          .toTry
          .get

      val diffAsJson            = encodeHtmlDiff(diff)
      val diffDecoded: HtmlDiff =
        decodeHtmlDiff.decodeJson(diffAsJson).toTry.get

      assertTrue(a == aDecoded && diff == diffDecoded)
    },
    test("Skip level") {
      val a = html(body(h1("Hello world from Lily!")))
      val b = html(body(h1("Hello world!")))

      val diff = HtmlDiff.unsafeOnlyBody(a, b)
      println(diff.asJson.spaces2)

      assertCompletes
    }
  )

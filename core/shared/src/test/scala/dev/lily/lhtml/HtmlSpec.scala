package dev.lily.lhtml

import zio.*
import zio.test.*
import dev.lily.lhtml.syntax.{*, given}

object HtmlSpec extends ZIOSpecDefault:
  def spec = suite("HtmlSpec")(
    test("basics") {
      val basic = element(
        "html",
        element("head", title(text("Hello world"))),
        body(
          h1("Hello world from Lily!"),
          div(
            div(
              div(
                div("Hello nested")
              )
            )
          ),
          div(
            div(text("World")).withId("test").withClass("hello").withData("lily-test" -> "true")
          ),
          ul(li("Hello"), li("World"))
        ).withClass("main")
          .withClass("test")
          .withData("lily-test" -> "true")
      )

      val html       = Html.renderWithIndent(basic)
      val htmlAsJson = Html.renderToJson(basic)

      println(html)

      assertTrue(
        html.contains("Hello world") && html.startsWith("<html>") &&
          htmlAsJson.contains("Hello world") && htmlAsJson.contains("body")
      )
    }.when(false),
    test("IDs") {
      val myHtml = html(
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
      // println(myHtml)
      // println("---")
      // println(HtmlIdEnhancer.addIds(myHtml))

      println(
        Html.renderWithIndent(
          Html.attachLIIDs(myHtml, 2332)
        )
      )

      // println(Html.renderWithIndent(HtmlIdEnhancer.addIds(myHtml)))

      assertCompletes
    }
  )

package dev.lily.examples

import dev.lily.ClientEvent.{on, onData}
import dev.lily.HTMLOps.{*, given}
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.{ClientEvent, LiveView}
import zio.ZIO.logWarning
import zio.http.Path
import zio.{Task, UIO, ZIO}

final case class MyTable(
  rows: Int = 7,
  columns: Int = 5,
  data: Map[(Int, Int), Option[String]] = Map.empty
)

object TableExample extends LiveView[Any, MyTable]:
  def state: UIO[MyTable] = ZIO.succeed(
    MyTable(data = Map((1, 1) -> Some("10"), (1, 2) -> Some("30"), (2, 1) -> Some("32"), (3, 2) -> Some("11")))
  )

  override def onEvent(state: MyTable, event: ClientEvent): ZIO[Any, Throwable, MyTable] = event match
    case onData("set", Some(value), List(row, column)) =>
      ZIO.succeed(state.copy(data = state.data + ((row.toInt, column.toInt) -> Some(value))))
    case on("addRow", _)                               =>
      ZIO.succeed(state.copy(rows = state.rows + 1))
    case on("addColumn", _)                            =>
      ZIO.succeed(state.copy(columns = state.columns + 1))
    case on("removeColumn", _)                         =>
      ZIO.succeed(state.copy(columns = state.columns - 1))
    case on("removeRow", _)                            =>
      ZIO.succeed(state.copy(rows = state.rows - 1))
    case on("populate", _)                             =>
      val random  = scala.util.Random
      val newData =
        (for r <- 1 to state.rows; c <- 1 to state.columns yield (r, c) -> Some(random.between(1, 1001).toString)).toMap
      ZIO.succeed(state.copy(data = newData))
    case e                                             => logWarning(s"Unhandled event: $e").as(state)

  private val cell: (Int, Int) => MyTable => Html = (r, c) =>
    table =>
      val cellInput = input()
        .attr("type" -> "number")
        .on("change", "set", List(r.toString, c.toString))

      table.data.get((r, c)) match
        case Some(maybeValue) => cellInput.attr("value" -> maybeValue.getOrElse(""))
        case None             => cellInput.attr("value" -> "")

  private def sumForColumn(table: MyTable, c: Int): Html =
    val sum = table.data.filter((pos, _) => pos._2 == c).values.flatMap(_.flatMap(_.toIntOption)).sum
    if sum == 0 then span(" ") else span(s"Sum = $sum")

  override def render(state: MyTable, path: Path): Task[Html] = ZIO.succeed:
    html(
      head(
        title("Hello tables"),
        style("""
                |html, body { font-family: sans-serif; }
                |input,td,th,p { font-size: 10pt; }
                |table td, table th { padding: 5px; }
                |table thead th { text-align: center; }
                |table input[type=number], table td, table th { text-align: right; }
                |table tbody tr:nth-child(even) td { background-color: #EEE; }
                |table { border-collapse: collapse; }
                |table tr td { border-top: 1px solid #EEE; }
                |.tools button { margin-right: 10px; } """.stripMargin)
      ),
      bodyOn(path)(
        h1("Lily - Table example"),
        p(a("&laquo; Back to examples").attr("href", "/")),
        div(
          button("Add row").on("click", "addRow"),
          button("Remove row").on("click", "removeRow"),
          button("Add column").on("click", "addColumn"),
          button("Remove column").on("click", "removeColumn"),
          button("Populate").on("click", "populate")
        ).klass("tools"),
        div(
          table(
            thead(
              tr(th("") +: (for (c <- 1 to state.columns) yield th((c + 64).toChar.toString)))
            ),
            tbody(
              for (r <- 1 to state.rows)
                yield tr(td(r.toString) +: (for (c <- 1 to state.columns) yield td(cell(r, c)(state))))
            ),
            tfoot(
              tr(
                td("") +: (for (c <- 1 to state.columns) yield td(sumForColumn(state, c)))
              )
            )
          )
        )
      )
    )

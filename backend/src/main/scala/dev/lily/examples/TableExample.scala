package dev.lily.examples

import dev.lily.ClientEvent.{on, onData}
import dev.lily.HTMLOps.{*, given}
import dev.lily.lhtml.Html
import dev.lily.lhtml.syntax.{*, given}
import dev.lily.{ClientEvent, LiveView}
import zio.ZIO.{logInfo, logWarning}
import zio.http.Path
import zio.stream.ZStream
import zio.{Ref, Task, ZIO}

final case class MyTable(
  rows: Int = 10,
  columns: Int = 7,
  data: Map[(Int, Int), Option[String]] = Map.empty
)

object TableExample:
  def make: ZIO[Any, Nothing, TableExample] =
    for tableRef <-
        Ref.make(
          MyTable(
            data = Map(
              (1, 1) -> Some("10"),
              (1, 2) -> Some("30"),
              (2, 1) -> Some("32"),
              (3, 2) -> Some("11"),
              (3, 3) -> Some("32"),
              (3, 5) -> Some("55")
            )
          )
        )
    yield new TableExample(tableRef)

final case class TableExample private (private val tableRef: Ref[MyTable]) extends LiveView[Any, MyTable]:
  def state = ZStream.fromZIO(tableRef.get)

  override def onEvent(state: MyTable, event: ClientEvent): Task[MyTable] = event match
    case onData("set", Some(value), List(row, column)) =>
      tableRef.updateAndGet(_.copy(data = state.data + ((row.toInt, column.toInt) -> Some(value))))
    case on("addRow", _)                               =>
      tableRef.updateAndGet(_.copy(rows = state.rows + 1))
    case on("addColumn", _)                            =>
      tableRef.updateAndGet(_.copy(columns = state.columns + 1))
    case on("removeColumn", _)                         =>
      tableRef.updateAndGet(_.copy(columns = state.columns - 1))
    case on("removeRow", _)                            =>
      tableRef.updateAndGet(_.copy(rows = state.rows - 1))
    case on("clear", _)                                =>
      tableRef.updateAndGet(_.copy(data = Map.empty))
    case on("populate", _)                             =>
      val random  = scala.util.Random
      val newData =
        (for r <- 1 to state.rows; c <- 1 to state.columns yield (r, c) -> Some(random.between(1, 1001).toString)).toMap
      tableRef.updateAndGet(_.copy(data =
        val oldData = state.data
        newData.foldLeft(oldData) { case (acc, (pos, value)) =>
          acc + (pos -> value)
        }
      ))
    case onData("removeRowAt", _, List(row))           =>
      tableRef.updateAndGet(_.copy(data = state.data.filterNot(_._1._1 == row.toInt)))
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
    Examples.layout(Some("Table example"), Some(path))(
      h1("Lily - Table example"),
      div(
        button("Add row").on("click", "addRow"),
        button("Remove row").on("click", "removeRow"),
        button("Add column").on("click", "addColumn"),
        button("Remove column").on("click", "removeColumn"),
        button("Populate").on("click", "populate"),
        button("Clear").on("click", "clear")
      ).klass("tools"),
      div(
        table(
          thead(
            tr(th("").attr("colspan" -> "2") +: (for (c <- 1 to state.columns) yield th((c + 64).toChar.toString)))
          ),
          tbody(
            for (r <- 1 to state.rows)
              yield tr(
                Seq(td(r.toString), td(button("🗑️").on("click", "removeRowAt", List(r.toString)))) ++
                  (for (c <- 1 to state.columns) yield td(cell(r, c)(state)))
              )
          ),
          tfoot(
            tr(
              td("").attr("colspan" -> "2") +: (for (c <- 1 to state.columns) yield td(sumForColumn(state, c)))
            )
          )
        )
      )
    )

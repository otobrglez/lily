package dev.lily.sandbox

import scala.annotation.targetName

object Utils:
  // infix def printLine[T](input: T): Unit                      = println(input)
  @targetName("printLineAny") def printLine(input: Any): Unit = println(input)

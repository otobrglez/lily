package dev.lily.sandbox.lambdacalculus.untyped

// Identity function
// λx. x
val id: Any => Any = x => x

// Self-application
// λf. f f
val self: (Any => Any) => Any = f => f(f)

// Booleans
// TRUE = λt.λf.t
// FALSE = λt.λf.f
val TRUE: Any => Any => Any                      = t => _ => t
val FALSE: Any => Any => Any                     = _ => f => f
val IF: (Any => Any => Any) => Any => Any => Any = p => a => b => p(a)(b)

// 4.1. Church Numerals
// 0 = λf.λx.x
// 1 = λf.λx.f x
// 2 = λf.λx.f(f x)
type ChurchNumeral = (Int => Int) => Int => Int
val ZERO: ChurchNumeral                                  = _ => x => x
val ONE: ChurchNumeral                                   = f => x => f(x)
val TWO: ChurchNumeral                                   = f => x => f(f(x))
val SUCC: ChurchNumeral => ChurchNumeral                 = n => f => x => f(n(f)(x))
val ADD: ChurchNumeral => ChurchNumeral => ChurchNumeral = m => n => f => x => m(f)(n(f)(x))

// 4.2. Church Numerals (Typed)
type CN[N] = (N => N) => N => N
def ZERO_T[N]: CN[N]                  = _ => x => x
def ONE_T[N]: CN[N]                   = f => x => f(x)
def TWO_T[N]: CN[N]                   = f => x => f(f(x))
def SUCC_T[N]: CN[N] => CN[N]         = n => f => x => f(n(f)(x))
def ADD_T[N]: CN[N] => CN[N] => CN[N] = m => n => f => x => m(f)(n(f)(x))

// 5. Pairs (Tuples)
val PAIR: Any => Any => (Any => Any => Any) => Any = x => y => f => f(x)(y)
val FIRST: ((Any => Any => Any) => Any) => Any     = _(x => _ => x)
val SECOND: ((Any => Any => Any) => Any) => Any    = _(_ => y => y)

// Y-Combinator

// Lambda calculus notation
// Y = λf. (λx. f (x x)) (λx. f (x x))

// Untyped Y-Combinator
lazy val YUN: ((Int => Int) => Int => Int) => Int => Int = f => x => f(YUN(f))(x)

// Typed Y-Combinator
def Y[A, B](f: (A => B) => A => B): A => B = x => f(Y(f))(x)

// Factorial with Y-Combinator
val factorial: Int => Int = Y: self =>
  case 0 => 1
  case n => n * self(n - 1)

// Sum range with Y-Combinator
val sumRange: Int => Int => Int = Y: self =>
  start =>
    case end if start > end => 0
    case end                => start + self(start + 1)(end)

// String length with Y-Combinator
val strLen: String => Int = Y: self =>
  case "" => 0
  case s  => 1 + self(s.tail)

@main def main(): Unit =
  println("--- " * 2 + "String Length with Y Combinator" + " ---" * 2)
  println("--- " * 2 + "Lambda Calculus 1" + " ---" * 2)
  println("### Booleans")
  println(IF(TRUE)("yes")("no"))
  println(IF(TRUE)("yes"))
  println(IF(TRUE))
  println(IF)

  println("### Church Numerals")
  val toInt: ChurchNumeral => Int = _(_ + 1)(0)
  println(toInt(ZERO))
  println(toInt(ONE))
  println(toInt(SUCC(ONE)))
  println(toInt(SUCC(SUCC(ONE))))
  println(toInt(SUCC(SUCC(SUCC(ONE)))))

  println("### Church Numerals (typed with char)")
  val toChar: CN[Char] => Char = _(c => (c + 1).toChar)('A')

  println(toChar(ZERO_T[Char]))
  println(toChar(ONE_T[Char]))
  println(toChar(TWO_T[Char]))

  println("### Pairs")
  val p = PAIR(1)('A')
  println(FIRST(p))
  println(SECOND(p))

  println("### Y-Combinator functions usage")
  println(factorial(6))           // 720
  println(sumRange(1)(5))         // 15 (1 + 2 + 3 + 4 + 5)
  println(strLen("Hello World!")) // 12

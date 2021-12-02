package part1recap

import scala.concurrent.Future

object AdvancedRecap extends App{

  //Partial Functions
  val partialFunction:PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val pf = (x: Int) => x match {
    case 1 => 42
    case 2 => 65
    case 5 => 999
  }

  val function: (Int => Int) = partialFunction

  val modifiedList = List(1,2,3).map{
    case 1 => 42
    case _ => 0
  }

  //Lifting
  val lifted = partialFunction.lift
  lifted(2) // Some(65)
  lifted(100) // None

  //orElse
  val pfChain = partialFunction.orElse[Int, Int]{
    case 60 => 90000
  }

  pf(5) // 999 as per partial functio,
  pf(60) // 90000
  pf(5000) // throw a matcher exception

  //type alias
  type ReceiveFunction = PartialFunction[Any, Unit]
  def receive: ReceiveFunction = {
    case 1 => println("hello")
    case _ => println("confused")
  }

  // Implicits
  implicit val timeout = 3000
  def setTimeOut(f: () => Unit)(implicit timeout: Int) = f()

  setTimeOut(() => println("Timeout")) // extra parameter list omitted

  // Implicit conversions
  // 1) implicit def
  case class Person(name: String){
    def greet = s"Hi my name is $name"
  }

  implicit def fromStringToPerson(str: String):Person = Person(str)

  "Peter".greet //fromStringToPerson("Peter").greet - automatically done by compiler

  // 2) implicit classes
  implicit class Dog(name: String){
    def bark = println("bark")
  }
  "Lassie".bark // new Dog("Lassie").bark - automatically done by compiler

  //organize
  //local scope
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,2,3).sorted //List(3,2,1)

  //imported scope
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future{
    println("hello future")
  }

  //Companion objects of the types included in the call
  object Person{
    implicit val personOrdering:Ordering[Person] = Ordering.fromLessThan((a,b) => a.name.compareTo(b.name) < 0)
  }

  List(Person("Bob"), Person("Alice")).sorted
  // List(Person("Alice"), Person("Bob"))
}

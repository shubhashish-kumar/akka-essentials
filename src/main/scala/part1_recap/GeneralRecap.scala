package part1_recap

object GeneralRecap extends App {

  val aCondition: Boolean = false

  var aVariable: Int = 42
  aVariable += 1 //aVariable = 43

  //Expressions
  val aConditionedVal = if(aCondition) 42 else 43

  //Code block
  val aCodeBlock = {
    if(aCondition) 43
    else 56
  }

  //types
  //Unit

  val theUnit = println("Hello World")

  def aFunction(x: Int): Int = x + 1

  //Recursion -- tailRecursion
  def factorial(n: Int, acc: Int): Int = {
    if(n == 0) acc
    else factorial(n - 1, acc * n)
  }

  //Object oriented programming
  class Animal
  class Dog extends Animal
  val aDog: Animal = new Dog

  trait Carnivore {
    def eat(a: Animal): Unit
  }

  class Crocodile extends Animal with Carnivore{
    override def eat(a: Animal): Unit = println("Crunch")
  }

  //Method notation
  val aCroc = new Crocodile
  aCroc.eat(aDog)
  aCroc eat aDog


  //Anonymous classes
  val aCarnivore = new Carnivore {
    override def eat(a: Animal): Unit = println("Roar")
  }

  aCarnivore eat aDog

  //Generics
  abstract class MyList[+A]
  //Companion object
  object MyList

  //Case class
  case class Person(name: String, age: Int)

  //Exceptions
  val aPotentialFailure = {
    try{
      throw new RuntimeException("its a runtime exception") //Nothing
    }catch {
      case e:Exception => "Caught an exception"
    }finally {
      //side effects
      println("Some logs")
    }
  }

  //Functional Programming
  val incrementer = new Function[Int, Int] {
    override def apply(v1: Int): Int = v1 + 1
  }

  val incremented = incrementer(42) //43
  //incrementer.apply(42)

  val anonymousIncrementer = (x: Int) => x + 1
  //Int => Int ===== Function1[Int, Int]

  //Functional Programming is all about working with functions as first class
  List(1,2,3).map(incrementer)
  //map = Higher Order Function

  //for comprehension
  val pair = for{
    num <- List(1,2,3)
    char <- List('a', 'b', 'c')
  }yield num + "-" + char
  //List(1,2,3).flatMap(num => List('a', 'b', 'c').map(char => num + "-" + char))

  //Seq, Array, List, Vector, Map, Tuples, Sets

  //Collections
  //Option and Try
  val anyOption = Some(2)
  import scala.util.Try
  val aTry = Try{
    throw new RuntimeException
  }

  //Pattern Matching
  val unknown = 2
  val order = unknown match {
    case 1 => "First"
    case 2 => "Second"
    case _ => "Unknown"
  }

  val bob = new Person("Bob", 23)
  val greeting = bob match {
    case Person(name, _) => s"Hi, my name is $name"
    case _ => "I don't know my name"
  }
}

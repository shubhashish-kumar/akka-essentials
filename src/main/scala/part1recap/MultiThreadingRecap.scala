package part1recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MultiThreadingRecap extends App {
  //creating threads on the JVM
  val aThread: Thread = new Thread(() => println("I am running in parallel"))
  aThread.start()
  aThread.join()

  val threadHello = new Thread(() => (1 to 1000).foreach(_ => println("hello")))
  val threadGoodBye = new Thread(() => (1 to 1000).foreach(_ => println("goog bye")))
  threadHello.start()
  threadGoodBye.start()

  //different runs produce different results
  class BankAccount(@volatile private var amount: Int){
    override def toString: String = "" + amount
    def withdraw(money: Int) = this.amount -= money
    def safeWithdraw(money: Int) = this.synchronized{
      this.amount -= money
    }
  }

  //Inter thread communication on the JVM
  //wait notify mechanism

  //Scala Futures
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future{
    //long computation - on a different thread
    42
  }

  //callbacks
  future.onComplete{
    case Success(42) => println("I found the meaning of life.")
    case Failure(_) => println("Something happened with the meaning of life.")
  }

  val aProcessedFuture = future.map(_ + 1) // Future with 43
  val aFlatFuture = future.flatMap{ value =>
    Future(value + 2)
  }// Future with 44

  val filteredFuture = future.filter(_ % 2== 0) // NoSuchElementException

  //for comprehension
  val aNonSenseFuture = for{
    meaningOfLife <- future
    filteredMeaning <- filteredFuture
  } yield meaningOfLife + filteredMeaning
}

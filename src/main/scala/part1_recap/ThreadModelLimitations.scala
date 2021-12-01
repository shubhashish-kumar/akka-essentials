package part1_recap

import scala.concurrent.Future

object ThreadModelLimitations extends App {
  /**
   * # 1: OOP encapsulation is only valid in the SINGLE THREADED MODEL.
   */
  class BankAccount(var amount: Int){
    override def toString: String = "" + amount
    def withdraw(money: Int) = this.synchronized{
      amount -= money
    }

    def deposit(money: Int) = this.synchronized{
      amount += money
    }

    def getAmount = amount
  }

  /**
   *  # 2: Delegating something to a thread is a PAIN.
   */
  // we have a running thread
  // we have to pass a runnable to that thread
  var task: Runnable = null

  var runningThread: Thread = new Thread(() => {
    while(true){
      while(task == null){
        runningThread.synchronized{
          println("[Background] Waiting for task...")
          runningThread.wait()
        }
      }

      task.synchronized{
        println("[Background] I have a task...")
        task.run()
        task = null
      }
    }
  })

  def delegateToBackgroundThread(r: Runnable) = {
    if(task == null) task = r

    runningThread.synchronized{
      runningThread.notify()
    }
  }

  runningThread.start()
  Thread.sleep(1000)
  delegateToBackgroundThread(() => println(42))
  Thread.sleep(1000)
  delegateToBackgroundThread(() => println("This should run in background."))

  /**
   * # 3: Tracing and dealing with errors in a multithreaded environment is a PAIN IN THE NECK.
   */
  // 1M numbers between 10 threads
  import scala.concurrent.ExecutionContext.Implicits.global
  val futures = (0 to 9)
    .map(i => 100000 * i until 100000 * (i + 1)) // 0 - 99999, 100000 - 199999, 200000 - 199999 etc
    .map(range => Future{
      if(range.contains(546738)) throw new RuntimeException("Invalid number")
      range.sum
    })

  val sumFuture = Future.reduceLeft(futures)(_ + _) // Future with the sum of all ranges
  sumFuture.onComplete(println)
}

package part5akkainfrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {
  class Counter extends Actor with ActorLogging {
    var count = 0
    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

    val system = ActorSystem("DispatcherDemo") //ConfigFactor.load().getConfig("dispatcherDemo")

    // Pragmatic in code
    val actors = for (i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
    val r = new Random()
    for (i <- 1 to 100) {
      actors(r.nextInt(10)) ! i
    }

    // method #2 - from config
//    val rtjvm = system.actorOf(Props[Counter], "rtjvm")


    /**
     * Dispatcher implements the ExecutionContext trait
     */

    class DatabaseActor extends Actor with ActorLogging {
      // solution #1
      implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher") // context.dispatcher
      // solution #2 - use routers

      override def receive: Receive = {
        case message => Future {
          // wait on a resource
          Thread.sleep(5000)
          log.info(s"Success: $message")
        }
      }
    }

    val dbActor = system.actorOf(Props[DatabaseActor])
    dbActor ! "the meaning of life is 42"

    val nonBlockingActor = system.actorOf(Props[Counter])
    for (i <- 1 to 100) {
      val message = s"important message $i"
      dbActor ! message
      nonBlockingActor ! message
    }
}
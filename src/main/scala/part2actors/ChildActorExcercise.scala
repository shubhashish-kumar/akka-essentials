package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorExcercise extends App{

  // Distributed word counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }

  class WordCounterMaster extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) =>
        println("[Master] Initializing...")
        val childrenRefs = for(i <- 0 to (nChildren - 1)) yield context.actorOf(Props[WordCounterWorker], s"WordCounterWorker-$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
    }

    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[Master] I have received content < $text > and I will send it to child $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val nextTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, nextTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"[${self.path}] I have received taskId $id with content $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }



  class TestActor extends Actor{
    import WordCounterMaster._
    override def receive: Receive = {
      case "go" =>
        val wordCounterMaster = context.actorOf(Props[WordCounterMaster], "WordCounterMaster")
        wordCounterMaster ! Initialize(3)
        val texts = List("Akka is awesome", "Scala is super dope", "Yes", "I like it too")
        texts.foreach(wordCounterMaster ! _)
      case count: Int => println(s"[Test Actor] I have received a count: $count")
    }
  }
  val system = ActorSystem("RoundRobinWordCountExercise")
  val testActor = system.actorOf(Props[TestActor], "testActor")
  testActor ! "go"


  /**
   * create WordCounterMaster
   * send Initialize(10) to WordCounterMaster
   * send "Akka is awesome to WordCounterMaster
   *  WCM will send a WordCounterTask("...") to one of its child.
   *    child reply with WordCountReply(3) to the master
   *  master replies with 3 to the sender
   *
   * requester -> WCM -> WCW
   * requester <- WCM <- WCW
   *
   * round robin logic
   * 1,2,3,4,5 and 7 tasks
   * 1,2,3,4,5,1,2
   */

}

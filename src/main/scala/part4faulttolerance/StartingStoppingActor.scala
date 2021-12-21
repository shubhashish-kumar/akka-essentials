package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActor extends App{

  val system = ActorSystem("StartingStoppingActorsDemo")

  object Parent{
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }
  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child with name $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"stopping child with name $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
      case Stop =>
        log.info("Stopping myself")
        context.stop(self)
      case message => log.info(message.toString)
    }
  }
  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * Method #1: using context.stop(_)
   */
  import Parent._
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")
  val child = system.actorSelection("user/parent/child1")
  child ! "Hi kid"

  parent ! StopChild("child1")
  for(_ <- 1 to 50) child ! "Are you still there"

  parent ! StartChild("child2")
  val child2 = system.actorSelection("user/parent/child2")
  child2 ! "Hi second child"
  parent ! Stop
  for(_ <- 1 to 10) parent ! "Parent, are you still there" //should not be received
  for(i <- 1 to 100) child2 ! s"[$i]Child2, are you still there"

  /**
   * Method #2 : Using special messages
   */
  val looseActor = system.actorOf(Props[Child])
  looseActor ! "Hello, loose actor"
  looseActor ! PoisonPill
  looseActor ! "Loose actor, Are you still there"

  val abruptlyTerminatedActor = system.actorOf(Props[Child])
  abruptlyTerminatedActor ! "You are about to be terminated"
  abruptlyTerminatedActor ! Kill
  abruptlyTerminatedActor ! "You are terminated"

  /**
   * Method #3 : Death watch
   */

  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"the reference that I'm watching $ref has been stopped")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")

  Thread.sleep(500)
  watchedChild ! PoisonPill
}

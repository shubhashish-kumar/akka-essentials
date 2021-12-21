package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import com.sun.tools.javac.comp.Check

object ActorLifeCycle extends App {

  case object StartChild
  class LifeCycleActor extends Actor with ActorLogging{
    override def preStart(): Unit = log.info("I am starting")
    override def postStop(): Unit = log.info("I am stopping")
    override def receive: Receive = {
      case StartChild => context.actorOf(Props[LifeCycleActor], "child")
    }
  }
  val system = ActorSystem("LifeCycleDemo")
  val parent = system.actorOf(Props[LifeCycleActor], "Parent")
  parent ! StartChild
  parent ! PoisonPill

  /**
   * restart
   */
  object Fail
  object FailChild
  object CheckChild
  object Check

  class Parent extends Actor with ActorLogging {
    private val supervisedChild = context.actorOf(Props[Child], "SupervisedChild")
    override def receive: Receive = {
      case FailChild => supervisedChild ! Fail
      case CheckChild => supervisedChild ! Check
    }
  }

  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("Supervised child started")
    override def postStop(): Unit = log.info("Supervised child stopped")
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info(s"Supervised child restarting because $reason")
    override def postRestart(reason: Throwable): Unit = log.info("Supervised child restarted")
    override def receive: Receive = {
      case Fail =>
        log.warning("Supervised child will fail")
        throw new RuntimeException("I failed")
      case Check => log.info("I am alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "Supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild
}

package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
with ImplicitSender
with WordSpecLike
with BeforeAndAfterAll{

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

    import SupervisionSpec._

    "A supervisor" should {
      "resume its child in case of minor fault" in {
        val supervisor = system.actorOf(Props[Supervisor])
        supervisor ! Props[FussyWordCounter]
        val child = expectMsgType[ActorRef]

        child ! "I love akka"
        child ! Report
        expectMsg(3)

        child ! "Akka is awesome because I am learning to think in a new way"
        child ! Report
        expectMsg(3)
      }

      "restart its child in case of an empty sentence" in {
        val supervisor = system.actorOf(Props[Supervisor])
        supervisor ! Props[FussyWordCounter]
        val child = expectMsgType[ActorRef]

        child ! "Akka is awesome"
        child ! Report
        expectMsg(3)

        child ! ""
        child ! Report
        expectMsg(0)
      }

      "terminate its child in case of a major error" in {
        val supervisor = system.actorOf(Props[Supervisor])
        supervisor ! Props[FussyWordCounter]
        val child = expectMsgType[ActorRef]

        watch(child)
        child ! "akka is awesome"

        val terminated = expectMsgType[Terminated]
        assert(terminated.actor == child)
      }

      "escalate an error when it doesn't know what to do" in {
        val supervisor = system.actorOf(Props[Supervisor])
        supervisor ! Props[FussyWordCounter]
        val child = expectMsgType[ActorRef]

        watch(child)
        child ! 43

        val terminated = expectMsgType[Terminated]
        assert(terminated.actor == child)
      }
  }

  "A kinder supervisor" should {
    "not kill its children in case its restarted or escalates failure" in {
      val kinderSupervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "supervisor")
      kinderSupervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 43
      child ! Report
      expectMsg(0)
    }
  }

  "An all-for-one supervisor" should {
    "apply the all for one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "allForOneSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child1 = expectMsgType[ActorRef]

      supervisor ! Props[FussyWordCounter]
      val child2 = expectMsgType[ActorRef]

      child2 ! "Testing supervision"
      child2 ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        child1 ! ""
      }

      Thread.sleep(500)
      child2 ! Report
      expectMsg(0)
    }
  }
}

object SupervisionSpec {

  class Supervisor extends Actor {

    override val supervisorStrategy : SupervisorStrategy = OneForOneStrategy(){
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      //empty
    }
  }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy: SupervisorStrategy = AllForOneStrategy(){
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  case object Report

  class FussyWordCounter extends Actor {
    var words = 0
    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("Sentence can not be empty")
      case sentence: String =>
        if(sentence.length > 20) throw new RuntimeException("Sentence is too big")
        else if(!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("Sentence should start with upper case letter")
        else words += sentence.split(" ").length
      case _ => throw new Exception("can only receive string")
    }
  }

}

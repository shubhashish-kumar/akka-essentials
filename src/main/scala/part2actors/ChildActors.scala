package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}
import part2actors.ChildActors.Parent.{CreateChild, TellChild}

object ChildActors extends App {

  //Actors can create other actors

  object Parent{
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }
  class Parent extends Actor{
    import Parent._
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"[${self.path}] creating child")
        // create a child actor right here
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => childRef forward message
    }
  }

  class Child extends Actor{
    override def receive: Receive = {
      case message: String => println(s"[${self.path}] I got : $message")
    }
  }

  import Parent._
  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "Parent")
  parent ! CreateChild("Child")
  parent ! TellChild("Hey Kid")


  // actor hierarchies
  // parent -> child -> grand child
  //        -> child2 ->

  /**
   * Guardian actor top-level
   * 1. /system -> system guardian
   * 2. /user -> user-level guardian
   * 3. / -> the root guardian
   */

  /**
   * Actor selection
   */
  val childSelection = system.actorSelection("/user/Parent/Child")
  childSelection ! "I found you"

  /**
   * Danger !
   *
   * NEVER PASS MUTABLE ACTOR STATE OR THE `THIS` REFERENCE TO THE CHILD ACTORS
   */

  object NaiveBankAccount{
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }
  class NaiveBankAccount extends Actor{
    import NaiveBankAccount._
    import CreditCard._
    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) // !!
      case Deposit(fund) => deposit(fund)
      case Withdraw(fund) => withdraw(fund)
    }

    def deposit(fund: Int) = amount += fund
    def withdraw(fund: Int) = amount -= fund
  }

  object CreditCard{
    case class AttachToAccount(bankAccount: NaiveBankAccount) // !!
    case object CheckStatus
  }
  class CreditCard extends Actor{

    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachTo(account))
    }

    def attachTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
          println(s"[${self.path}] Your message has been processed.")
        // benign
        account.withdraw(1) // because I can
    }
  }

  import NaiveBankAccount._
  import CreditCard._

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(1000)

  Thread.sleep(1000)

  val creditCardSelection = system.actorSelection("/user/account/card")
  creditCardSelection ! CheckStatus
}

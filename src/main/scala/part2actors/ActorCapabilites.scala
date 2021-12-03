package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorCapabilites.Person.LiveTheLife

object ActorCapabilites extends App{
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi !" => sender() ! s"[${self}] Hello there !" //replying to a message
      case message: String => println(s"[${context.self}]I have received a message: $message")
      case number:Int => println(s"[Simple Actor] I have received a number: $number")
      case SpecialMessage(content) => println(s"[Simple Actor] I have received a special message: $content")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi !" // <=> (ref ! "Hi !")(self) alice is being passed as sender
      case WirelessPhoneMessage(content, ref) => ref forward content + "s" // I keep the original sender of WPM
    }
  }

  val system = ActorSystem("ActorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  // 1. message can be of anytype
  // a. message must be IMMUTABLE
  // b. message must be SERIALIZABLE
  simpleActor ! "Hello actor"
  simpleActor ! 42 // who is the sender
  //in practice, use case classes and case objects
  case class SpecialMessage(content: String)
  simpleActor ! SpecialMessage("Its special")

  // 2. actors have information about their context and about themselves
  // context.self === `this` in OOP
  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and I am proud of it")

  // 3. actors can reply to a message
  val alice = system.actorOf(Props[SimpleActor], "Alice")
  val bob = system.actorOf(Props[SimpleActor], "Bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4. Dead Letters
  alice ! "Hi !"

  // 5. Forwarding messages
  // D -> A -> B
  // Forwarding -> sending a message with the ORIGINAL sender.
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi !", bob) //no sender

  /**
   * Exercises
   *
   * 1. a Counter actor
   *   - Increment
   *   - Decrement
   *   - Print
   *
   * 2. a Bank account as an actor
   *   receives :
   *   - Deposit an amount
   *   - Withdraw an amount
   *   - Statement
   *
   *   replies with :
   *   - Success / Failure
   *
   *   interact with other kind of actor
   */


  // 1. Counter actor

  // Domain of the counter
  object CounterActor{
    case object Increment
    case object Decrement
    case object Print
  }

  class CounterActor extends Actor{
    import CounterActor._
    var counter = 0
    override def receive: Receive = {
      case Increment => counter += 1
      case Decrement => counter -= 1
      case Print => println(s"[Counter Actor] The counter is now: $counter")
    }
  }

  import CounterActor._
  val counterActor = system.actorOf(Props[CounterActor], "CounterActor")
  (0 to 10).foreach(_ => counterActor ! Increment)
  (0 to 5).foreach(_ => counterActor ! Decrement)
  counterActor ! Print

  // 2. Bank Account actor
  // Domain of the Bank account
  object BankAccount{
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
  }

  class BankAccount extends Actor{
    import BankAccount._
    var balance = 0
    override def receive: Receive = {
      case Deposit(amount) =>
        if(amount < 0) sender() ! TransactionFailure(s"Invalid amount for deposit i.e.: $amount")
        else {
          balance += amount
          sender() ! TransactionSuccess(s"Deposit successfully with an amount: $amount")
        }
      case Withdraw(amount) =>
        if(amount < 0) sender() ! TransactionFailure(s"Invalid amount for withdrawal i.e.: $amount")
        else if(amount > balance) sender() ! TransactionFailure(s"You don't have sufficient balance for requested amount : $amount")
        else {
          balance -= amount
          sender() ! TransactionSuccess(s"Withdrawal successfully for amount: $amount")
        }
      case Statement => sender() ! println(s"[Bank Account Actor] The current balance in the account is: $balance")
    }
  }

  object Person{
    case class LiveTheLife(bankAccount: ActorRef)
  }

  class Person extends Actor{
    import Person._
    import BankAccount._
    override def receive: Receive = {
      case LiveTheLife(account: ActorRef) =>
        account ! Deposit(-100)
        account ! Deposit(1000)
        account ! Withdraw(10000)
        account ! Withdraw(-10)
        account ! Withdraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val account = system.actorOf(Props[BankAccount], "BankAccount")
  val person = system.actorOf(Props[Person], "Person")

  person ! LiveTheLife(account)
}

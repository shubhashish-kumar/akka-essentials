package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehaviour.Mom.MomStart

object ChangingActorBehaviour extends App {

  object FussyKid{
    case object KidAccept
    case object KidReject

    val HAPPY = "happy"
    val SAD ="sad"

  }
  class FussyKid extends Actor{
    import FussyKid._
    import Mom._
    //internal state of the kid
    var state = HAPPY
    override def receive: Receive = {
      case Food(CHOCOLOTE) => state = HAPPY
      case Food(VEGETABLES) => state = SAD
      case Ask(_) =>
        if(state == HAPPY) sender() ! KidAccept else sender() ! KidReject

    }
  }

  class StatelessFussyKid extends Actor{
    import FussyKid._
    import Mom._
    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive, false) //change my receive handler to sadReceive
      case Food(CHOCOLOTE) =>
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive, false)
      case Food(CHOCOLOTE) =>  context.unbecome() //change my receive handler to happyReceive
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom{
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String)

    val CHOCOLOTE = "chocolate"
    val VEGETABLES = "veggies"
  }
  class Mom extends Actor{
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLES)
        kidRef ! Food(VEGETABLES)
        kidRef ! Food(CHOCOLOTE)
        kidRef ! Food(CHOCOLOTE)
        kidRef ! Ask("Are you happy?")
      case KidAccept => println("Yay! my kid is happy.")
      case KidReject => println(("My kid is sad"))
    }
  }

  val system = ActorSystem("changingActorBehaviour")
  val mom = system.actorOf(Props[Mom], "Mom")
  val kid = system.actorOf(Props[FussyKid], "Kid")
  val statelessKid = system.actorOf(Props[StatelessFussyKid], "StatelessKid")

  mom ! MomStart(kid)
  mom ! MomStart(statelessKid)

  /**
   * mom receive start
   *  kid receive Food(Veg) => kid will change the handler to sadReceive
   *  kid receive Ask(_) => kid reples with sadReceive handler
   * mom receives KidReject
   */

  /**
   * context.become
   *  Food(Veg) => stack.push(sadReceive)
   *  Food(Chocolate) => stack.push(happyReceive)
   *
   * stack:
   *  1. happyReceive
   *  2. sadReceive
   *  3. happyReceive
   */

  /**
   * context.unbecome
   *  new behaviour
   *  Food(Veg)
   *  Food(Veg)
   *  Food(Chocolate)
   *  Food(Chocolate)
   *
   *  stack:
   *    1. happyReceive
   */

  /**
   * Excercise 1: Recreate the Counter Actor with context become and NO MUTABLE STATE
   */

  object CounterActor{
    case object Increment
    case object Decrement
    case object Print
  }
  class CounterActor extends Actor{
    import CounterActor._
    override def receive: Receive = countReceive(0)

    def countReceive(currentCount: Int): Receive = {
      case Increment =>
        println(s"[ countReceive($currentCount) ] incrementing")
        context.become(countReceive(currentCount + 1))
      case Decrement =>
        println(s"[ countReceive($currentCount) ] decrementing")
        context.become(countReceive(currentCount - 1))
      case Print =>
        println(s"[ countReceive($currentCount) ] Current counter value is : $currentCount")
    }
  }

  import CounterActor._
  val counterActor = system.actorOf(Props[CounterActor], "CounterActor")
  (0 to 7).foreach( _ => counterActor ! Increment)
  (0 to 5).foreach( _ => counterActor ! Decrement)
  counterActor ! Print

  /**
   * Excercise 2: Simplified voting system
   */

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])
  /*class Citizen extends Actor{
    var candidate: Option[String] = None
    override def receive: Receive = {
      case Vote(c)  => candidate = Some(c)
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }*/
  class Citizen extends Actor{

    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }

    override def receive: Receive = {
      case Vote(candidate)  => context.become(voted(candidate))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  /*class VoteAggregator extends Actor{
    var stillWaiting: Set[ActorRef] = Set()
    var currentStats: Map[String, Int] = Map()
    override def receive: Receive = {
      case AggregateVotes(citizens) =>
      stillWaiting = citizens
      citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
      case VoteStatusReply(None) =>
        sender() ! VoteStatusRequest
      case  VoteStatusReply(Some(candidate)) =>
        val newWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        currentStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if(newWaiting.isEmpty)
          println(s"[Aggregator] Poll Stats: $currentStats")
        else
          stillWaiting = newWaiting
    }
  }*/

  class VoteAggregator extends Actor{
    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(_ ! VoteStatusRequest)
        context.become(awaitingStatus(citizens, Map()))
    }

    def awaitingStatus(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]) : Receive = {
      case VoteStatusReply(None) =>
        //a citizen has not yet voted
        sender() ! VoteStatusRequest
      case  VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        val newStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if(newStillWaiting.isEmpty)
          println(s"[Aggregator] Poll Stats: $newStats")
        else {
          //still need to process some statuses from some other citizens
          context.become(awaitingStatus(newStillWaiting, newStats))
        }
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

  /**
   * Print the status of the votes
   *
   * Martin -> 1
   * Jonas -> 1
   * Roland -> 2
   */

}

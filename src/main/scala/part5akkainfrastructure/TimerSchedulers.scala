package part5akkainfrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

object TimerSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
  val system = ActorSystem("TimerSchedulersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  system.log.info("Scheduling reminder of simpleActor")

//  implicit val executionContext: ExecutionContext = system.dispatcher
  import system.dispatcher
  system.scheduler.scheduleOnce(1 second){
    simpleActor ! "reminder"
  }

  val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds){
    simpleActor ! "heart beat"
  }

  system.scheduler.scheduleOnce(5 seconds){
    routine.cancel()
  }

  /**
   * Excercise: Implement a self closing actor
   *
   *  - If the actor receives a message(anything), you have 1 second to send it another message
   *  - If the time window expire, the actor will stop itself
   *  - If you send another message, the window will reset
   */

  class SelfClosingActor extends Actor with ActorLogging {
    var schedule = createTimeoutWindow()

    def createTimeoutWindow(): Cancellable =
      context.system.scheduler.scheduleOnce(1 second){
        self ! "timeout"
      }

    override def receive: Receive = {
      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"received $message, staying alive")
        schedule.cancel()
        schedule = createTimeoutWindow()
    }
  }

  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
  system.scheduler.scheduleOnce(250 millis){
    selfClosingActor ! "ping"
  }

  system.scheduler.scheduleOnce(2 seconds){
    system.log.info("sending pong to the self closing actor")
    selfClosingActor ! "pong"
  }

  /**
   * Timer
   */
  case object TimerKey
  case object Start
  case object Stop
  case object Reminder
  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)
    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.warning("Stopping myself")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerBasedHeartBeatActor = system.actorOf(Props[TimerBasedHeartBeatActor], "timerBasedHeartBeatActor")
  system.scheduler.scheduleOnce(5 second){
    timerBasedHeartBeatActor ! Stop
  }
}


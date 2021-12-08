package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {
  /**
   * 1. EXplicit Logging
   */
  class SimpleLoggerWithExplicitLogger extends Actor {
    val logger = Logging(context.system, this)
    override def receive: Receive = {
      /**
       * 1. DEBUG
       * 2. INFO
       * 3. WARNING
       * 4. ERROR
       */
      case message => logger.info(message.toString) //LOG IT
    }
  }
  val system = ActorSystem("LoggingDemo")
  val explicitLoggerActor = system.actorOf(Props[SimpleLoggerWithExplicitLogger], "ExplicitLogger")
  explicitLoggerActor ! "Logging a simle message"

  /**
   * 2. Actor Logging
   */
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Received two things: {} and {}", a, b) //Received two things: 34 and 55
      case message => log.info(message.toString)
    }
  }

  val simpleActorWithActorLogging = system.actorOf(Props[ActorWithLogging], "ImplicitLogger")
  simpleActorWithActorLogging ! (34, 55)
  simpleActorWithActorLogging ! "Logging a simple message by extending ActorLogging trait"
}

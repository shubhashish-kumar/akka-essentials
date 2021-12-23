package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /**
   * ResourceActor
      - open => it can receive read/write requests to the resource
      - otherwise it will postpone all read/write requests until the state is open
      ResourceActor is closed
        - Open => switch to the open state
        - Read, Write messages are POSTPONED
      ResourceActor is open
        - Read, Write are handled
        - Close => switch to the close state
      [Open, Read, Read, Write]
      - switch to the open state
      - read the data,
      - read the data again
      - write the data
      [Read, Open, Write]
      - stash Read
        Stash: [Read]
      - open => switch to the open state
        Mailbox: [Read, Write]
      - read and write are handled
   */

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  //Step 1: Mix-in the Stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    var innerData: String = ""
    override def receive: Receive = close()

    def close(): Receive = {
      case Open =>
        log.info("Opening resource")
        //step #3: unstashAll when you switch to message handler
        unstashAll()
        context.become(open())
      case message =>
        log.info(s"Stashing $message because I can't handle it while its closed")
        //step #2: Stash away, if you can't handle
        stash()
    }

    def open(): Receive = {
      case Read =>
        //do some actual computation
        log.info(s"I have read $innerData")
      case Write(data) =>
        log.info(s"I am writing $data")
        innerData = data
      case Close =>
        log.info("Closing resources")
        unstashAll()
        context.become(close())
      case message =>
        log.info(s"Stashing $message because I can't handle it while its open")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor], "resourceActor")
  resourceActor ! Read //Stashed
  resourceActor ! Open //Switched to open state; I have read
  resourceActor ! Open //Stashed
  resourceActor ! Write("I love stash") //I am writing "I love stash"
  resourceActor ! Close //switch to close, switch to open
  resourceActor ! Read //I have read "I love stash"
}

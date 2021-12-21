package part4faulttolerance

import akka.actor.SupervisorStrategy.{Stop, stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import java.io.File
import scala.io.Source
import scala.concurrent.duration._
import scala.language.postfixOps

object BackOffSupervisionPattern extends App {

  case object ReadFile
  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit = log.info("Persistent actor is starting.")

    override def postStop(): Unit = log.warning("Persistent actor stopped.")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info("Persistent actor is restarting.")
    override def receive: Receive = {
      case ReadFile =>
        if(dataSource == null) {
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
          log.info(s"I have just read some important data: ${dataSource.getLines().toList}")
        }
    }
  }

  val system = ActorSystem("BackOffSupervisorDemo")
  val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
  simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackoffActor",
      3 seconds, // then 6s, 12s, 24s
      30 seconds,
      0.2
    )
  )

  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleBackoffSupervisor")
  simpleBackoffSupervisor ! ReadFile
  /**
   * simpleSupervisor
   *  - child called simpleBackoffSupervisor(Props of type FileBasedPersistentActor)
   *  - supervision strategy is default one (restarting on everything)
   *   - first attempt after 3 seconds
   *   - next attempt at 2x the previous attempt
   */
  val stopSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy(){
        case _ => Stop
      }
    )
  )
  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
  stopSupervisor ! ReadFile
  class EagerFBPActor extends FileBasedPersistentActor {
  override def preStart(): Unit = {
    log.info("Eager actor is starting.")
    dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
  }
}
  val eagerActor = system.actorOf(Props[EagerFBPActor], "eagerActor")
  //ActorInitializationException => STOP

  val repeatedSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      3 seconds,
      30 seconds,
      0.1
    )
  )
  val eagerFBPActor = system.actorOf(repeatedSupervisorProps, "eagerFBPActor")

  /**
   * eagerSupervisor
   *  - child eagerActor
   *   - will die on start with ActorInitializationException
   *   - trigger the supervisionStrategy  in eagerSupervisor => STOP eagerActor
   *  - backoff will kick in after 1 second, 2 seconds, 4 seconds, 8 seconds ...
   */
}

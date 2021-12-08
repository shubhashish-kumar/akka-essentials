package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroActorConfig extends App {

  class SimpleActorLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * 1. Inline config
   */
  val configString =
    """
      |akka{
      |loglevel = "INFO"
      |}
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", config)
  val actor = system.actorOf(Props[SimpleActorLogging], "SimpleActorLogging")
  actor ! "A message to remember"

  /**
   * 2. Config file
   */
  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileSystem")
  val defaultActor = defaultConfigFileSystem.actorOf(Props[SimpleActorLogging], "DefaultSimpleActorLogging")
  defaultActor ! "remember me"


  /**
   * 3. Separate config in the same file
   */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigActorSystem", specialConfig)
  val specialActor = specialConfigSystem.actorOf(Props[SimpleActorLogging], "SpecialActorLogging")
  specialActor ! "remember me! I am special"

  /**
   * 4. Separate config in another file
   */
  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"Separate config log level: ${separateConfig.getString("akka.loglevel")}")


  /**
   * 5. Different file formats
   * i.e. json, properties
   */
  val jsonConfig = ConfigFactory.load("json/jsonConfiguration.json")
  println(s"json config: ${jsonConfig.getString("aJSONProperty")}")
  println(s"json config: ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"properties config: ${propsConfig.getString("my.simpleProperty")}")
  println(s"properties config: ${propsConfig.getString("akka.loglevel")}")


}

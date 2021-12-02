package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  // Part 1 - Actor systems
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // Part 2 - Create actors
  // word count actor
  class WordCountActor extends Actor{
    //internal data
    var totalWords = 0
    //behavior
    override def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[Word Counter] - I have received a message: $message")
        totalWords += message.split(" ").length
        println(s"Total word count: $totalWords")
      case msg => println(s"[Word Counter] - I can not understand message: ${msg.toString}")
    }
  }

  // Part 3 - Instantiate our actors
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "WordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "AnotherWordCounter")

  // Part 4 - Communicate ! -- Asynchronous
  wordCounter ! "I am learning Akka and its damn cool !" //tell
  anotherWordCounter ! "It is just another message"

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }
  object Person{
    def props(name: String) = Props(new Person(name))
  }

  val personActor = actorSystem.actorOf(Person.props("Bob"))
  personActor ! "hi"

}

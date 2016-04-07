import akka.actor.{Props, ActorSystem}
import jmx.{KafkaMetrics, KafkaJMX}

object MonitorApp extends App {
  val actorSystem = ActorSystem.create("main")
  val monitor = actorSystem.actorOf(Props[MonitorActor])
  monitor ! InitMonitor
}

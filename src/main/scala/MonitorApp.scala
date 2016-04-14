import akka.actor.{ActorSystem, Props}

object MonitorApp {
  def main(args: Array[String]) {
    val zookeeper = if (args.length >= 1) args(0) else "localhost"
    val jmxPort = if (args.length >= 2) args(1).toInt else 9997
    val topicName = if (args.length >= 3) Option(args(2)) else Option.empty

    val actorSystem = ActorSystem.create("main")
    val props = Props(classOf[KafkaMonitorActor], zookeeper, jmxPort, topicName)
    val monitor = actorSystem.actorOf(props)
    monitor ! InitMonitor
  }
}

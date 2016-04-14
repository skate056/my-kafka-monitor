import akka.actor.{ActorSystem, Props}

object MonitorApp {
  def main(args: Array[String]) {
    val jmxHost = if (args.length >= 1) args(0) else "localhost"
    val jmxPort = if (args.length >= 2) args(1).toInt else 9997
    val topicName = if (args.length >= 3) args(2) else "upstream.thermostat"

    val actorSystem = ActorSystem.create("main")
    val props = Props(classOf[MonitorActor], jmxHost, jmxPort, topicName)
    val monitor = actorSystem.actorOf(props)
    monitor ! InitMonitor
  }
}

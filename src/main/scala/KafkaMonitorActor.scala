import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField._
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import jmx.MeterMetric

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

case object InitMonitor

case object RefreshStats

case object Done

class KafkaMonitorActor(zookeeperUrl: String, port: Int, topicName: Option[String]) extends Actor with ActorLogging {
  val oneSec: FiniteDuration = FiniteDuration(1, SECONDS)
  implicit val ec = ExecutionContext.Implicits.global

  val clusterProps = Props(classOf[KafkaClusterActor], zookeeperUrl)
  val clusterActor = context.actorOf(clusterProps)

  var jmxActor: ActorRef = null
  var printer: ActorRef = context.actorOf(Props[StatsPrinter])

  override def receive: Receive = {
    case InitMonitor =>
      log("Fetching broker list")
      clusterActor ! GetBrokerList
    case BrokerList(brokers) =>
      log(s"Brokers are: $brokers")
      val jmxProps = Props(classOf[KafkaJMXActor], brokers, port)
      jmxActor = context.actorOf(jmxProps)

      log(s"Setting up with $brokers:$port for topic $topicName")
      context.system.scheduler.schedule(oneSec, oneSec) {
        self ! RefreshStats
      }
    case RefreshStats => jmxActor ! FetchStats(topicName)
    case tss@TopicStatsSuccess(_, _) => printer ! tss
    case TopicStatsFailure(_, th) => println(th)
  }

  def log(str: String): Unit = log.debug(str)
}

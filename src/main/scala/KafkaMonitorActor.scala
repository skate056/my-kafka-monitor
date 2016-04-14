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

  val delimiter = "\t"
  val formatter = DateTimeFormatter.ISO_DATE_TIME
  val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
  //  val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
  val timeFormatter = new DateTimeFormatterBuilder()
    .appendValue(HOUR_OF_DAY, 2)
    .appendLiteral(':')
    .appendValue(MINUTE_OF_HOUR, 2)
    .optionalStart
    .appendLiteral(':')
    .appendValue(SECOND_OF_MINUTE, 2)
    .toFormatter()

  val clusterProps = Props(classOf[KafkaClusterActor], zookeeperUrl)
  val clusterActor = context.actorOf(clusterProps)

  var jmxActor: ActorRef = null

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
    case TopicStatsSuccess(_, mm) => println(ts + formatMsg(mm))
    case TopicStatsFailure(_, th) => println(th)
  }

  def formatMsg(mm: MeterMetric): String = {
    s"$delimiter offsetSum=${mm.count} $delimiter 1m=${mm.formatOneMinuteRate} $delimiter 5m=${mm.formatFiveMinuteRate} $delimiter 15m=${mm.formatFifteenMinuteRate}"
  }

  def ts: String = {
    val now = LocalDateTime.now()
    s"${dateFormatter.format(now)} ${timeFormatter.format(now)}"
  }

  def log(str: String): Unit = log.debug(str)
}

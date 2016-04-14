import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField._
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{Actor, Props}
import jmx.MeterMetric

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

case object InitMonitor

case object RefreshStats

case object Done

class MonitorActor(host: String, port: Int, topicName: String = "") extends Actor {
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

  val jmxProps = Props(classOf[KafkaJMXActor], host, port)
  val jmxActor = context.actorOf(jmxProps)

  override def receive: Receive = {
    case InitMonitor =>
      context.system.scheduler.schedule(oneSec, oneSec) {
        self ! RefreshStats
      }
    case RefreshStats => jmxActor ! FetchStats(topicName)
    case TopicStatsSucces(_, mm) => println(ts + formatMsg(mm))
    case TopicStatsFailure(_, th) => println(th)
  }

  def formatMsg(mm: MeterMetric): String = {
    s"$delimiter offsetSum=${mm.count} $delimiter 1m=${mm.formatOneMinuteRate} $delimiter 5m=${mm.formatFiveMinuteRate} $delimiter 15m=${mm.formatFifteenMinuteRate}"
  }

  def ts: String = {
    val now = LocalDateTime.now()
    s"${dateFormatter.format(now)} $delimiter ${timeFormatter.format(now)}"
  }
}

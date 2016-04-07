import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField._
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.Actor
import jmx.{KafkaJMX, KafkaMetrics, MeterMetric}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

case object InitMonitor

case object RefreshStats

case object Done

class MonitorActor extends Actor {
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

  override def receive: Receive = {
    case InitMonitor =>
      context.system.scheduler.schedule(oneSec, oneSec) {
        self ! RefreshStats
      }
    case RefreshStats =>
      val version = model.Kafka_0_9_0_0
      val result = KafkaJMX.doWithConnection("localhost", 9997, None, None) {
        mbsc => KafkaMetrics.getMessagesInPerSec(version, mbsc, Option("dp.hive.ukmap.raw"))
      }
      result match {
        case Success(mm) =>
          println(ts + formatMsg(mm))
        case Failure(exc) =>
          println(exc)
      }
  }

  def formatMsg(mm: MeterMetric): String = {
    s"$delimiter offsetSum=${mm.count} $delimiter 1m=${mm.formatOneMinuteRate} $delimiter 5m=${mm.formatFiveMinuteRate} $delimiter 15m=${mm.formatFifteenMinuteRate}"
  }

  def ts: String = {
    val now = LocalDateTime.now()
    s"${dateFormatter.format(now)} $delimiter ${timeFormatter.format(now)}"
  }
}

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField._

import akka.actor.Actor
import jmx.MeterMetric

class StatsPrinter extends Actor {
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
    case TopicStatsSuccess(_, mm) => println(ts + formatMsg(mm))
  }

  def formatMsg(mm: MeterMetric): String = {
    s"$delimiter offsetSum=${mm.count} $delimiter 1m=${mm.formatOneMinuteRate} $delimiter 5m=${mm.formatFiveMinuteRate} $delimiter 15m=${mm.formatFifteenMinuteRate}"
  }

  def ts: String = {
    val now = LocalDateTime.now()
    s"${dateFormatter.format(now)} ${timeFormatter.format(now)}"
  }
}

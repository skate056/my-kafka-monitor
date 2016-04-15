package mykafkamonitor

import java.time.LocalDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField._
import java.util.concurrent.atomic.AtomicLong

import akka.actor.Actor
import km.jmx.MeterMetric

case class Stats(topicName: String, meterMetric: MeterMetric, publishRate: Option[Long])

class StatsPrinterActor extends Actor {
  val Delimiter = "\t"

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

  val printCount = new AtomicLong()

  override def receive: Receive = {
    case Stats(_, mm, rate) =>
      val c = printCount.getAndIncrement()
      if (c % 10 == 0) {
        println(formatHeaderLine())
        println(formatHeader())
        println(formatHeaderLine())
      }
      println(formatMsg(mm, rate))
  }

  import FormatterUtils._

  def formatHeader(): String = {
    val timestamp = padTimestamp("Timestamp")
    val rate = padRate("Current Rate (m/s)")
    val oneMinRate = padRate("1 minute rate")
    val fiveMinRate = padRate("5 minute rate")
    val fifteenMinRate = padRate("15 minute rate")
    val meanRate = padRate("Mean rate")
    buildLine(timestamp, rate, oneMinRate, fiveMinRate, fifteenMinRate, meanRate)
  }

  def formatHeaderLine(): String = {
    val timestamp = padTimestamp("-------------------")
    val rate = padRate("------------------")
    val oneMinRate = padRate("------------------")
    val fiveMinRate = padRate("------------------")
    val fifteenMinRate = padRate("------------------")
    val meanRate = padRate("------------------")
    buildLine(timestamp, rate, oneMinRate, fiveMinRate, fifteenMinRate, meanRate)
  }

  def formatMsg(mm: MeterMetric, rawRate: Option[Long] = Option.empty): String = {
    val timestamp = padTimestamp(formattedTime)
    val rate = padRate(rawRate.map(_ + " m/s").getOrElse(""))
    val oneMinRate = padRate(formatHistoricalRate(mm.oneMinuteRate))
    val fiveMinRate = padRate(formatHistoricalRate(mm.fiveMinuteRate))
    val fifteenMinRate = padRate(formatHistoricalRate(mm.fifteenMinuteRate))
    val meanRate = padRate(formatHistoricalRate(mm.meanRate))
    buildLine(timestamp, rate, oneMinRate, fiveMinRate, fifteenMinRate, meanRate)
  }

  def buildLine(timestamp: String, currentRate: String, oneMinRate: String, fiveMinRate: String, fifteenMinRate: String, meanRate: String): String = {
    s"|$timestamp|$currentRate|$oneMinRate|$fiveMinRate|$fifteenMinRate|$meanRate|"
  }

  def formattedTime: String = {
    val now = LocalDateTime.now()
    s"${dateFormatter.format(now)} ${timeFormatter.format(now)}"
  }
}

object FormatterUtils {
  def formatHistoricalRate(num: Double): String = "% 8.2f".format(num)

  def formatCurrentRate(num: Double): String = "% 8.0f".format(num)

  def pad(len: Int)(str: String): String = {
    val strLen = str.length
    if (strLen > len) {
      throw new IllegalArgumentException(s"Field of length $strLen does not fit in $len")
    }
    if (strLen < len) {
      str.reverse.padTo(len, " ").reverse.mkString
    } else str
  }

  def padTimestamp: (String) => String = pad(19)

  def padRate: (String) => String = pad(18)

  def pad20: (String) => String = pad(20)
}

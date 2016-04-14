import akka.actor.{Actor, ActorLogging}
import jmx.{KafkaJMX, KafkaMetrics, MeterMetric}

import scala.util.{Failure, Success, Try}

case class FetchStats(topicName: Option[String])

case class TopicStatsSuccess(topicName: Option[String], meterMetric: MeterMetric)

case class TopicStatsFailure(topicName: Option[String], th: Throwable)

class KafkaJMXActor(hosts: Seq[String], port: Int) extends Actor with ActorLogging {
  val version = model.Kafka_0_9_0_0

  log.debug(s"Setup with $hosts and $port")

  override def receive: Receive = {
    case FetchStats(topicName) =>
      val results = hosts.map { host =>
        KafkaJMX.doWithConnection(host, port, None, None) {
          mbsc => KafkaMetrics.getMessagesInPerSec(version, mbsc, topicName)
        }
      }

      val mergedMetrics = mergeResults(results)
      mergedMetrics match {
        case Left(mm) => sender() ! TopicStatsSuccess(topicName, mm)
        case Right(th) => sender() ! TopicStatsFailure(topicName, th)
      }
  }

  private def mergeResults(results: Seq[Try[MeterMetric]]): Either[MeterMetric, Throwable] = {
    val ZERO: Either[MeterMetric, Throwable] = Left(MeterMetric(0, 0, 0, 0, 0))
    results.foldLeft(ZERO) {
      case (result@Right(_), _) => result
      case (result, Success(mm)) => result.left.map(add(_, mm))
      case (result, Failure(th)) => Right(th)
    }
  }

  def add(a: MeterMetric, b: MeterMetric): MeterMetric = {
    MeterMetric(
      a.count + b.count,
      a.fifteenMinuteRate + b.fifteenMinuteRate,
      a.fiveMinuteRate + b.fiveMinuteRate,
      a.oneMinuteRate + b.oneMinuteRate,
      a.meanRate + b.meanRate)
  }
}

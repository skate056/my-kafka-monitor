import akka.actor.Actor
import jmx.{KafkaJMX, KafkaMetrics, MeterMetric}

import scala.util.{Failure, Success}

case class FetchStats(topicName: String)

case class TopicStatsSucces(topicName: String, meterMetric: MeterMetric)

case class TopicStatsFailure(topicName: String, th: Throwable)

class KafkaJMXActor(host: String, port: Int) extends Actor {
  val version = model.Kafka_0_9_0_0

  override def receive: Receive = {
    case FetchStats(topicName) =>
      val result = KafkaJMX.doWithConnection(host, port, None, None) {
        mbsc => KafkaMetrics.getMessagesInPerSec(version, mbsc, Option(topicName))
      }

      result match {
        case Success(mm) =>
          val ts = TopicStatsSucces(topicName, mm)
          sender() ! ts
        case Failure(th) =>
          val tf = TopicStatsFailure(topicName, th)
          sender() ! tf
      }
  }
}

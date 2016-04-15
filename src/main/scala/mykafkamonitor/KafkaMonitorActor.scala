package mykafkamonitor

import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection._
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
  var offsetActor: ActorRef = null
  val printer: ActorRef = context.actorOf(Props[StatsPrinterActor])

  val publishRate: mutable.Map[String, Long] = mutable.Map()

  override def receive: Receive = {
    case InitMonitor =>
      logDebug("Fetching broker list")
      clusterActor ! GetBrokerList
    case BrokerList(brokers) =>
      logDebug(s"Brokers are: $brokers")
      val jmxProps = Props(classOf[KafkaJMXActor], brokers, port)
      jmxActor = context.actorOf(jmxProps)

      val offsetProps = Props(classOf[MessageRateActor], brokers, port)
      offsetActor = context.actorOf(offsetProps)

      logDebug(s"Setting up with $brokers:$port for topic $topicName")
      context.system.scheduler.schedule(oneSec, oneSec) {
        self ! RefreshStats
      }
    case RefreshStats =>
      jmxActor ! FetchStats(topicName)
      topicName.foreach(offsetActor ! GetPublishRate(_))
    case tss@TopicStatsSuccess(_, _) =>
      tss.topicName
        .map { t => Stats(t, tss.meterMetric, publishRate.get(t)) }
        .foreach(printer ! _)
    case TopicStatsFailure(_, th) => println(th)
    case PublishRateResponse(t, rate) => publishRate.update(t, rate)
  }

  def logDebug(str: String): Unit = log.debug(str)
}

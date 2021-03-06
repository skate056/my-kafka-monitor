package mykafkamonitor

import akka.actor.{Actor, ActorLogging}
import kafka.utils.ZkUtils
import org.apache.kafka.common.protocol.SecurityProtocol

case object GetBrokerList

case class BrokerList(brokers: Seq[String])

class KafkaClusterActor(zkUrl: String) extends Actor with ActorLogging {
  val zkUtils = ZkUtils(zkUrl, 10000, 10000, isZkSecurityEnabled = false)

  val resolvingMap = Map("c333072.home" -> "localhost")

  override def receive: Receive = {
    case GetBrokerList =>
      val brokers = zkUtils.getAllBrokersInCluster()
      val hosts = brokers.map { broker =>
        log.debug(s"Received endpoints ${broker.endPoints}")
        val host = broker.endPoints(SecurityProtocol.PLAINTEXT).host
        resolvingMap.getOrElse(host, host)
      }
      sender() ! BrokerList(hosts)
  }
}

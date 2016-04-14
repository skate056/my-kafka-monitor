package mykafkamonitor

import akka.actor.Actor

case class GetPublishRate(topicName: String)

case class PublishRateResponse(topicName: String, rate: Long)

class MessageRateActor(hosts: Seq[String], port: Int) extends Actor {
  override def receive: Receive = {
    case GetPublishRate(topicName) =>
      sender() ! PublishRateResponse(topicName, 99)
  }
}

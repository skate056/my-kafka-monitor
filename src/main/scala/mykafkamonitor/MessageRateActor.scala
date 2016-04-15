package mykafkamonitor

import akka.actor.Actor
import kafka.api._
import kafka.common.TopicAndPartition
import kafka.consumer.SimpleConsumer
import scala.collection._

case class GetPublishRate(topicName: String)

case class PublishRateResponse(topicName: String, rate: Long)

class MessageRateActor(hosts: Seq[String], port: Int) extends Actor {
  import KafkaConf._
  val prevOffset:mutable.Map[String, Long] = mutable.Map()

  override def receive: Receive = {
    case GetPublishRate(topicName) =>
      val sumOffsets = hosts.map { host =>
        val consumer = new SimpleConsumer(host, DefaultPort, SocketTimeout, SocketTimeout, ClientId)
        val topicMetadataRequest = new TopicMetadataRequest(Seq(topicName), 1)
        val response = consumer.send(topicMetadataRequest)
        val topicMetadata = response.topicsMetadata.find(_.topic == topicName)
        val partitionMetadata = topicMetadata.map(_.partitionsMetadata).toSeq.flatten
//        println(s"Topic $topicName has ${partitionMetadata.size} partitions")

        val time = -1
        val nOffsets = 1
        val pori = new PartitionOffsetRequestInfo(time, nOffsets)
        val topicAndPart = partitionMetadata
          .map(x => TopicAndPartition(topicName,x.partitionId))
          .map((_, pori))
          .toMap
        val offsetFetchRequest = new OffsetRequest(topicAndPart)
        val offResp = consumer.getOffsetsBefore(offsetFetchRequest)
        val offsets = offResp.partitionErrorAndOffsets
//        offsets.foreach { case (tp, por) =>
//          if (tp.partition == 50)
//            println(tp + " = " + por)
//        }

        val sum = offsets.values.map(_.offsets).toSeq.flatten.sum
//        println(s"sum = $sum")
        sum
      }

      val bigSum = sumOffsets.sum
      val prev = prevOffset.getOrElse(topicName, 0L)
      val rate = bigSum - prev
//      println(s"rate = $rate")
      prevOffset(topicName) = bigSum

      sender() ! PublishRateResponse(topicName, rate)
  }
}

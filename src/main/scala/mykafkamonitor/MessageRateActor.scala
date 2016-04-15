package mykafkamonitor

import akka.actor.{Actor, ActorLogging}
import kafka.api._
import kafka.common.TopicAndPartition
import kafka.consumer.SimpleConsumer

import scala.collection._

case class GetPublishRate(topicName: String)

case class PublishRateResponse(topicName: String, rate: Long)

class MessageRateActor(hosts: Seq[String], port: Int) extends Actor with ActorLogging {

  import KafkaConf._

  val prevOffset: mutable.Map[String, Long] = mutable.Map()

  override def receive: Receive = {
    case GetPublishRate(topicName) =>
      val offsetsByHosts = hosts.map { host =>
        val consumer = new SimpleConsumer(host, DefaultPort, SocketTimeout, SocketTimeout, ClientId)
        val partitionMetadata = fetchPartitionMetadata(topicName, consumer)

        val offsets = fetchLatestOffsets(topicName, consumer, partitionMetadata)
        val sum = offsets.values.map(_.offsets).toSeq.flatten.sum
        log.debug(s"Sum of offsets from $host = $sum")
        consumer.close()
        sum
      }

      val sumOfOffsets = offsetsByHosts.sum
      val prev = prevOffset.getOrElse(topicName, 0L)
      val rate = sumOfOffsets - prev
      prevOffset(topicName) = sumOfOffsets

      sender() ! PublishRateResponse(topicName, rate)
  }

  def fetchLatestOffsets(topicName: String, consumer: SimpleConsumer, partitionMetadata: scala.Seq[PartitionMetadata]): Map[TopicAndPartition, PartitionOffsetsResponse] = {
    val time = -1
    val nOffsets = 1
    val offsetRequestInfo = new PartitionOffsetRequestInfo(time, nOffsets)
    val topicAndPartition = partitionMetadata
      .map(x => TopicAndPartition(topicName, x.partitionId))
      .map((_, offsetRequestInfo))
      .toMap
    val offsetFetchRequest = new OffsetRequest(topicAndPartition)
    val offResp = consumer.getOffsetsBefore(offsetFetchRequest)
    offResp.partitionErrorAndOffsets
  }

  def fetchPartitionMetadata(topicName: String, consumer: SimpleConsumer): scala.Seq[PartitionMetadata] = {
    val topicMetadataRequest = new TopicMetadataRequest(Seq(topicName), 1)
    val response = consumer.send(topicMetadataRequest)
    val topicMetadata = response.topicsMetadata.find(_.topic == topicName)
    val partitionMetadata = topicMetadata.map(_.partitionsMetadata).toSeq.flatten
    log.debug(s"Topic $topicName has ${partitionMetadata.size} partitions")
    partitionMetadata
  }
}

package mykafkamonitor

object KafkaConf {
  val GroupId: String = "my-kafka-monitor"
  val ClientId: String = "my-kafka-monitor"
  val SocketTimeout: Int = Int.MaxValue
  val BufferSize: Int = Int.MaxValue
  val DefaultPort: Int = 9092
}

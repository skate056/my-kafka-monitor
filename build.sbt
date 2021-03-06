name := "my-kafka-monitor"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.14",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.14",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "org.json4s" %% "json4s-scalaz" % "3.2.11",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.12",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  "org.apache.kafka" %% "kafka" % "0.9.0.1" exclude("log4j","log4j") exclude("org.slf4j", "slf4j-log4j12") force(),
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

assemblyJarName in assembly := s"${name.value}.jar"
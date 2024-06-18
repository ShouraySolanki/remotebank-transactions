package com.remotebank.transactions

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.{SimpleStringEncoder, SimpleStringSchema}
import org.apache.flink.connector.file.sink.FileSink
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy

import java.util.Properties
import java.util.concurrent.TimeUnit

object TransactionBackupJob {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "localhost:9092")
    properties.setProperty("group.id", "backup-job")

    val kafkaSource = KafkaSource.builder[String]()
      .setBootstrapServers("kafka:9092")
      .setTopics("transactions")
      .setGroupId("backup-job")
      .setStartingOffsets(OffsetsInitializer.earliest())
      .setValueOnlyDeserializer(new SimpleStringSchema())
      .build()

    val transactionsStream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks[String](), "transactions-job-backup ")

    val fileSink = FileSink.forRowFormat(new Path("file:///backup"), new SimpleStringEncoder[String]("UTF-8"))
      .withRollingPolicy(DefaultRollingPolicy.builder()
        .withRolloverInterval(TimeUnit.MINUTES.toMillis(1))
        .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
        .withMaxPartSize(1024 * 1024 * 1024)
        .build())
      .build()
    print(fileSink.toString)
    transactionsStream.sinkTo(fileSink)

    env.execute("Transaction Backup Job")
  }
}

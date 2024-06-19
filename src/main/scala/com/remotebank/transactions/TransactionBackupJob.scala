package com.remotebank.transactions

import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringEncoder
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
    properties.setProperty("bootstrap.servers", "kafka:9092")
    properties.setProperty("group.id", "backup-job")
    properties.setProperty("schema.registry.url", "http://schema-registry:8082")
    properties.setProperty(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, classOf[Map[String, Any]].getName)

    val kafkaSource = KafkaSource.builder[java.util.Map[String, Any]]()
      .setBootstrapServers("kafka:9092")
      .setTopics("transactions")
      .setGroupId("backup-job")
      .setStartingOffsets(OffsetsInitializer.earliest())
      .setValueOnlyDeserializer(new CustomJsonSchemaDeserializationSchema)
      .build()

    val transactionsStream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks[java.util.Map[String, Any]](), "transactions-job-backup ")

    val fileSink = FileSink.forRowFormat(new Path("file:///backup"), new SimpleStringEncoder[java.util.Map[String, Any]]("UTF-8"))
      .withRollingPolicy(DefaultRollingPolicy.builder()
        .withRolloverInterval(TimeUnit.MINUTES.toMillis(1))
        .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
        .withMaxPartSize(1024 * 1024 * 1024)
        .build())
      .build()
    transactionsStream.sinkTo(fileSink)

    env.execute("Transaction Backup Job")
  }
}

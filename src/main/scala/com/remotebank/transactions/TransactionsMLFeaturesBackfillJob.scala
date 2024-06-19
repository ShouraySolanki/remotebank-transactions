package com.remotebank.transactions

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringEncoder
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.createTypeInformation
import org.apache.flink.connector.file.sink.FileSink
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.util.Collector
import org.json.JSONObject

import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit



object TransactionsMLFeaturesBackfillJob {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // Configuration variable to control backfill
    val performBackfill = false // Set to true to perform backfill, false for normal operation

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "kafka:9092")
    properties.setProperty("group.id", "ml-features-job")
    properties.setProperty("schema.registry.url", "http://schema-registry:8082")

    // Kafka Source
    val kafkaSource = KafkaSource.builder[java.util.Map[String, Any]]()
      .setBootstrapServers("kafka:9092")
      .setTopics("transactions")
      .setGroupId("ml-features-job")
      .setStartingOffsets(OffsetsInitializer.earliest())
      .setProperties(properties)
      .setValueOnlyDeserializer(new CustomJsonSchemaDeserializationSchema)
      .build()

    val transactionsStream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks[java.util.Map[String, Any]](), "transactions-ml-features-job")

    // File Sink for real-time and historical data
    val fileSink = FileSink.forRowFormat(new Path("file:///ml-features"), new SimpleStringEncoder[String]("UTF-8"))
      .withRollingPolicy(DefaultRollingPolicy.builder()
        .withRolloverInterval(TimeUnit.MINUTES.toMillis(1))
        .withInactivityInterval(TimeUnit.MINUTES.toMillis(1))
        .withMaxPartSize(1024 * 1024 * 1024)
        .build())
      .build()

    // Processing transactions to count total transactions per user
    val transactionCounts = processTransactions(transactionsStream)

    // Sink the transaction counts to file and Redis (only for real-time data)
    if (!performBackfill) {
      println(" real time event processing ")
      transactionCounts.map(_.toJson).sinkTo(fileSink)
      transactionCounts.addSink(new RedisSink())
    }

    // Process historical data if needed
    if (performBackfill) {
      println(" historical event processing ")
      val historicalTransactionsPath = "/backup"
      val historicalTransactionCounts = processHistoricalTransactions(env, historicalTransactionsPath)
      historicalTransactionCounts.map(_.toJson).sinkTo(fileSink)
    }

    env.execute("Transactions ML Features Job")
  }

  // Function to process real-time transactions
  def processTransactions(transactionsStream: DataStream[java.util.Map[String, Any]]): DataStream[UserTransactionCount] = {
    transactionsStream
      .map(record => {
        val json = new JSONObject(record)
        Transaction(json.getInt("user_id"), json.getLong("transaction_timestamp_millis"), json.getFloat("amount"), json.getString("currency"), json.getInt("counterpart_id"))
      })
      .keyBy(_.user_id)
      .process(new KeyedProcessFunction[Int, Transaction, UserTransactionCount] {
        lazy val state = getRuntimeContext.getState(new ValueStateDescriptor[Int]("transactionCount", classOf[Int]))

        override def processElement(transaction: Transaction, ctx: KeyedProcessFunction[Int, Transaction, UserTransactionCount]#Context, out: Collector[UserTransactionCount]): Unit = {
          val currentCount = Option(state.value()).getOrElse(0)
          val newCount = currentCount + 1
          state.update(newCount)
          out.collect(UserTransactionCount(transaction.user_id, newCount))
        }
      })
  }


  def processHistoricalTransactions(env: StreamExecutionEnvironment, basePath: String): DataStream[UserTransactionCount] = {
    val files = listFiles(basePath)
    val streams = files.map(file => env.readTextFile(file.getPath))
    val historicalTransactionsStream = streams.reduce(_.union(_))

    historicalTransactionsStream
      .map(record => {
        val json = new JSONObject(record)
        Transaction(json.getInt("user_id"), json.getLong("transaction_timestamp_millis"), json.getFloat("amount"), json.getString("currency"), json.getInt("counterpart_id"))
      })
      .keyBy(_.user_id)
      .process(new KeyedProcessFunction[Int, Transaction, UserTransactionCount] {
        lazy val state: ValueState[Int] = getRuntimeContext.getState(new ValueStateDescriptor[Int]("transactionCount", classOf[Int]))

        override def processElement(transaction: Transaction, ctx: KeyedProcessFunction[Int, Transaction, UserTransactionCount]#Context, out: Collector[UserTransactionCount]): Unit = {
          val currentCount = Option(state.value()).getOrElse(0)
          val newCount = currentCount + 1
          state.update(newCount)
          out.collect(UserTransactionCount(transaction.user_id, newCount))
        }
      })
  }

  def listFiles(directory: String): Seq[File] = {
    val dir = new File(directory)
    if (dir.exists && dir.isDirectory) {
      dir.listFiles().flatMap { dateDir =>
        if (dateDir.isDirectory) {
          dateDir.listFiles().filter(_.getName.startsWith(".part-"))
        } else {
          Array.empty[File]
        }
      }
    } else {
      Seq.empty[File]
    }
  }
}

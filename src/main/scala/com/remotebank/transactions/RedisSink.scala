package com.remotebank.transactions

import org.apache.flink.streaming.api.functions.sink.SinkFunction
import redis.clients.jedis.Jedis


class RedisSink extends SinkFunction[UserTransactionCount] {
  @transient lazy val jedis = new Jedis("redis", 6379)

  override def invoke(value: UserTransactionCount, context: SinkFunction.Context): Unit = {
    jedis.set(s"user:${value.user_id}:total_transactions_count", value.total_transactions_count.toString)
  }
}

package com.remotebank.transactions

case class Transaction(user_id: Int, transaction_timestamp_millis: Long, amount: Float, currency: String, counterpart_id: Int)

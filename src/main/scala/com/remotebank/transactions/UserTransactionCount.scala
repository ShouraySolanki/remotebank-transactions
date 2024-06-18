package com.remotebank.transactions

case class UserTransactionCount(user_id: Int, total_transactions_count: Int) {
  def toJson: String = {
    s"""{"user_id": $user_id, "total_transactions_count": $total_transactions_count}"""
  }
}
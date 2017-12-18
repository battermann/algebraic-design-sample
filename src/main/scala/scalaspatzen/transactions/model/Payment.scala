package scalaspatzen.transactions.model

import org.joda.time.DateTime

sealed trait TransactionType

final case class Payment(
  id: String,
  date: DateTime,
  sender: String,
  amount: BigDecimal
)

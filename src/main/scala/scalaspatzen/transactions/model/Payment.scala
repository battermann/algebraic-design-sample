package scalaspatzen.transactions.model

import org.joda.time.DateTime

final case class Payment(
  id: String,
  date: DateTime,
  sender: String,
  amount: BigDecimal
)

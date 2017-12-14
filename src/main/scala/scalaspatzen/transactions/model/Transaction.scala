package scalaspatzen.transactions.model

import org.joda.time.DateTime

sealed trait TransactionType

object Debit extends TransactionType
object Credit extends TransactionType

final case class Transaction(
  date: DateTime,
  transactionType: TransactionType,
  description: String,
  sender: String,
  receiver: String,
  amount: BigDecimal
)

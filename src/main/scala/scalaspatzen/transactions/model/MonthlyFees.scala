package scalaspatzen.transactions.model

import org.joda.time.Interval

case class MonthlyFees(
  interval: Interval,
  tuition: BigDecimal,
  foodAllowance: BigDecimal
)
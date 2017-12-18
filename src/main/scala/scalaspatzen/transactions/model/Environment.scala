package scalaspatzen.transactions.model

import org.joda.time.Interval

case class MonthlyFees(
  interval: Interval,
  tuition: BigDecimal,
  foodAllowance: BigDecimal
)

case class Environment(
  paymentsDueDayOfMonth: Int,
  yearlyFee: BigDecimal,
  monthlyFees: List[MonthlyFees],
  debitors: List[Debitor]
)
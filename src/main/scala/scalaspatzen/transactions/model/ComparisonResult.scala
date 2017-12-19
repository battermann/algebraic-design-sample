package scalaspatzen.transactions.model

import cats.kernel.Monoid
import org.joda.time.Interval
import cats.implicits._
import com.github.nscala_time.time.Imports._

case class ComparisonResult(
    isAggregate: Boolean,
    interval: Option[Interval],
    yearlyFee: BigDecimal,
    tuition: BigDecimal,
    foodAllowance: BigDecimal,
    actualPayments: List[Payment],
    extraPayments: BigDecimal
) {
  def total(implicit amountMonoid: Monoid[BigDecimal]): BigDecimal =
    tuition |+| foodAllowance |+| yearlyFee
  def actualAmountPayed(implicit amountMonoid: Monoid[BigDecimal]): BigDecimal =
    actualPayments.foldMap(_.amount) |+| extraPayments
  def diff(implicit amountMonoid: Monoid[BigDecimal]): BigDecimal =
    actualAmountPayed |+| (-total)
}

object ComparisonResult {
  def empty(implicit m: Monoid[BigDecimal]): ComparisonResult = ComparisonResult(
    isAggregate = false,
    interval = None,
    yearlyFee = 0,
    tuition = 0,
    foodAllowance = 0,
    actualPayments = Nil,
    extraPayments = m.empty
  )

  private def min(lhs: DateTime, rhs: DateTime): DateTime = {
    if (rhs isAfter lhs) lhs
    else rhs
  }

  private def max(lhs: DateTime, rhs: DateTime): DateTime = {
    if (lhs isBefore rhs) rhs
    else lhs
  }

  implicit def comparisonResultMonoid(
      implicit amountMonoid: Monoid[BigDecimal]): Monoid[ComparisonResult] =
    new Monoid[ComparisonResult] {
      def empty: ComparisonResult = ComparisonResult.empty
      def combine(x: ComparisonResult,
                  y: ComparisonResult): ComparisonResult = {
        val interval = List(x.interval, y.interval) match {
          case List(Some(ix), Some(iy)) =>
            Some(min(ix.start, iy.start) to max(ix.end, iy.end))
          case foo @ _ => foo.flatten.headOption
        }
        ComparisonResult(
          isAggregate = true,
          interval = interval,
          x.yearlyFee |+| y.yearlyFee,
          x.tuition |+| y.tuition,
          x.foodAllowance |+| y.foodAllowance,
          x.actualPayments |+| y.actualPayments,
          x.extraPayments |+| y.extraPayments
        )
      }
    }
}

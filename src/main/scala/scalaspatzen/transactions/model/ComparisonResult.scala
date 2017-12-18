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
    actualAmountPayed: BigDecimal
) {
  def total(implicit amountMonoid: Monoid[BigDecimal]) = tuition |+| foodAllowance
  def diff(implicit amountMonoid: Monoid[BigDecimal]) = actualAmountPayed |+| (-total)
}

object ComparisonResult {
  def empty: ComparisonResult = ComparisonResult(
    isAggregate = false,
    interval = None,
    yearlyFee = 0,
    tuition = 0,
    foodAllowance = 0,
    actualAmountPayed = 0,
  )

  private def min(lhs: DateTime, rhs: DateTime): DateTime = {
    if(rhs isAfter lhs) lhs
    else rhs
  }

  private def max(lhs: DateTime, rhs: DateTime): DateTime = {
    if(lhs isBefore rhs) rhs
    else lhs
  }

  implicit def comparisonResultMonoid(implicit amountMonoid: Monoid[BigDecimal]): Monoid[ComparisonResult] =
    new Monoid[ComparisonResult] {
      override def empty = ComparisonResult.empty
      override def combine(x: ComparisonResult, y: ComparisonResult) = {
        val interval = List(x.interval, y.interval) match {
          case List(Some(ix), Some(iy)) => Some(min(ix.start, iy.start) to max(ix.end, iy.end))
          case foo@_ => foo.flatten.headOption
        }
        ComparisonResult(
          isAggregate = true,
          interval = interval,
          x.yearlyFee |+| y.yearlyFee,
          x.tuition |+| y.tuition,
          x.foodAllowance |+| y.foodAllowance,
          x.actualAmountPayed |+| y.actualAmountPayed
        )
      }
    }
}

package scalaspatzen.transactions.interpreters

import java.util.UUID

import cats.Monoid
import cats.implicits._
import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, Interval}

import scala.util.Try
import scalaspatzen.transactions.algebra.Analyzer
import scalaspatzen.transactions.model._

object AnalyzerInterpreter
    extends Analyzer[Debitor,
                     Payment,
                     BigDecimal,
                     (BigDecimal, List[MonthlyFees]),
                     ComparisonResult] {

  private val formatter = DateTimeFormat.forPattern("dd.MM.yy")

  private def removeQuotes(str: String): String = {
    str.replace("\"", "")
  }

  def decode(line: String): Option[Payment] = {
    val elements = line.split("\";\"").map(removeQuotes)
    for {
      dateTime <- Try(formatter.parseDateTime(elements.head)).toOption
      amount <- Try(
        elements
          .drop(6)
          .head
          .replace(".", "")
          .replace(",", ".")
          .replace("€", "")
          .trim)
        .map(BigDecimal(_))
        .toOption
      sender <- elements.drop(4).headOption
      payment <- if (amount > 0) {
        Payment(UUID.nameUUIDFromBytes(line.toString.getBytes).toString,
                dateTime,
                sender,
                amount).some
      } else {
        None
      }
    } yield payment
  }

  def groupByDebitor(
      debitors: List[Debitor]): List[Payment] => Map[Debitor, List[Payment]] =
    (tans: List[Payment]) => {
      tans.distinct
        .flatMap { tan =>
          debitors
            .find(d =>
              d.normalizedSenderIds.exists(id =>
                tan.sender.toLowerCase.contains(id)))
            .map(d => (d, tan))
        }
        .groupBy(_._1)
        .mapValues(_.map(_._2))
    }

  private def getDueInterval(dt: DateTime,
                             paymentsDueDayOfMonth: Int): Interval = {
    val withCorrectMonth =
      if (dt.dayOfMonth().get <= paymentsDueDayOfMonth)
        dt
      else
        dt.plusMonths(1)
    val start = withCorrectMonth
      .withDayOfMonth(1)
      .withTimeAtStartOfDay()
    val end = withCorrectMonth
      .plusMonths(1)
      .withDayOfMonth(1)
      .withTimeAtStartOfDay()
    start to end
  }

  def groupByTimeInterval(paymentsDueDayOfMonth: Int)
    : TransactionsPerDebitor => Map[Debitor, Map[Interval, List[Payment]]] =
    (tansPerDebitor: TransactionsPerDebitor) =>
      tansPerDebitor.mapValues { v =>
        v.map(tan => (getDueInterval(tan.date, paymentsDueDayOfMonth), tan))
          .groupBy(_._1)
          .mapValues(_.map(_._2))
    }

  private def compare(debitor: Debitor,
                      payableAmounts: (BigDecimal, List[MonthlyFees]),
                      tans: Map[Interval, List[Payment]])(
      implicit m: Monoid[BigDecimal]): List[ComparisonResult] = {
    val (yearlyFee, monthlyFees) = payableAmounts
    val intervals = (monthlyFees.map(_.interval) ++ tans.keys).distinct
    val results = intervals
      .map { interval =>
        val (tuitionExpected, foodExpected) = monthlyFees
          .find(_.interval == interval)
          .map { f =>
            val tuition = if (debitor.tuitionSuspended.contains(interval)) {
              m.empty
            } else {
              f.tuition
            }
            val foodAllowance =
              if (debitor.foodAllowanceSuspended.contains(interval)) {
                m.empty
              } else {
                f.foodAllowance
              }
            (tuition, foodAllowance)
          }
          .getOrElse((m.empty, m.empty))
        val actualPayments = tans.getOrElse(interval, Nil)
        ComparisonResult(isAggregate = false,
                         interval.some,
                         0,
                         tuitionExpected * debitor.children.size,
                         foodExpected * debitor.children.size,
                         actualPayments,
                         extraPayments = m.empty)
      }
      .sortBy(_.interval.map(_.start).getOrElse(new DateTime(0))) match {
      case Nil => Nil
      case h :: t =>
        h.copy(yearlyFee = yearlyFee, extraPayments = debitor.extraPayments) :: t
    }
    val total = results.combineAll
    results :+ total
  }

  def compare(debitors: List[Debitor],
              payableAmounts: (BigDecimal, List[MonthlyFees]))
    : TransactionsPerIntervalPerDebitor => Map[Debitor,
                                               List[ComparisonResult]] =
    (tans: TransactionsPerIntervalPerDebitor) => {
      debitors
        .map(d => (d, compare(d, payableAmounts, tans.getOrElse(d, Map()))))
        .toMap
    }
}

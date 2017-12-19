package scalaspatzen.transactions.algebra

import org.joda.time.Interval

import scala.language.higherKinds

trait Analyzer[Debitor, Transaction, Amount, PayableAmounts, ComparisonResult] {

  type TransactionsPerDebitor = Map[Debitor, List[Transaction]]
  type TransactionsPerIntervalPerDebitor =
    Map[Debitor, Map[Interval, List[Transaction]]]
  type ComparisonResults = Map[Debitor, List[ComparisonResult]]
  type RawLine = String

  def decode(line: RawLine): Option[Transaction]

  def groupByDebitor(
      debitors: List[Debitor]): List[Transaction] => TransactionsPerDebitor

  def groupByTimeInterval(paymentsDueDayOfMonth: Int)
    : TransactionsPerDebitor => TransactionsPerIntervalPerDebitor

  def compare(debitors: List[Debitor], payableAmounts: PayableAmounts)
    : TransactionsPerIntervalPerDebitor => ComparisonResults

  // composed operations

  def decodeLines: List[RawLine] => List[Transaction] =
    _.flatMap(decode).distinct
}

package scalaspatzen.transactions.algebra

import cats.Monoid
import cats.implicits._
import org.joda.time.Interval
import scala.language.higherKinds

trait Analyzer[Debitor, Transaction, Amount] {

  type TransactionsPerDebitor = Map[Debitor, List[Transaction]]
  type TransactionsPerIntervalPerDebitor =
    Map[Debitor, Map[Interval, List[Transaction]]]
  type AmountPerIntervalPerDebitor = Map[Debitor, Map[Interval, Amount]]
  type RawLine = String

  def decode(line: RawLine): Option[Transaction]

  def getAmount(tan: Transaction): Amount

  def groupByDebitor(
      debitors: List[Debitor]): List[Transaction] => TransactionsPerDebitor

  def groupByTimeInterval
    : TransactionsPerDebitor => TransactionsPerIntervalPerDebitor

  def format(tans: AmountPerIntervalPerDebitor): String

  // composed operations

  def decodeLines: List[RawLine] => List[Transaction] = _.flatMap(decode)

  def sumAmounts(implicit m: Monoid[Amount])
    : TransactionsPerIntervalPerDebitor => AmountPerIntervalPerDebitor =
    _.mapValues(x => x.mapValues(_.map(getAmount).combineAll))
}

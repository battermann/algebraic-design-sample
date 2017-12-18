package scalaspatzen.transactions.interpreters

import java.util
import java.util.UUID

import cats.Monoid

import scalaspatzen.transactions.algebra.Analyzer
import scalaspatzen.transactions.model._
import com.github.nscala_time.time.Imports._
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.typographic.TypographicExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.{Parser, ParserEmulationProfile}
import com.vladsch.flexmark.util.options.MutableDataSet
import org.joda.time.Interval
import org.joda.time.format.DateTimeFormat
import cats.implicits._
import org.joda.time.DateTime

import scala.util.Try

object AnalyzerInterpreter
    extends Analyzer[Debitor,
                     Payment,
                     BigDecimal,
                     (BigDecimal, List[MonthlyFees]),
                     ComparisonResult] {
  private def removeQuotes(str: String): String = {
    str.replace("\"", "")
  }

  private val formatter = DateTimeFormat.forPattern("dd.MM.yy")

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

  def getAmount(tan: Payment): BigDecimal = tan.amount

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
    : AnalyzerInterpreter.TransactionsPerDebitor => Map[
      Debitor,
      Map[Interval, List[Payment]]] =
    (tansPerDebitor: TransactionsPerDebitor) =>
      tansPerDebitor.mapValues { v =>
        v.map(tan => (getDueInterval(tan.date, paymentsDueDayOfMonth), tan))
          .groupBy(_._1)
          .mapValues(_.map(_._2))
    }

  private def compare(debitor: Debitor,
                      payableAmounts: (BigDecimal, List[MonthlyFees]),
                      tans: Map[Interval, BigDecimal])(
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
        val amountPayed = tans.getOrElse(interval, m.empty)
        ComparisonResult(isAggregate = false,
                         interval.some,
                         0,
                         tuitionExpected,
                         foodExpected,
                         amountPayed)
      }
      .sortBy(_.interval.map(_.start).getOrElse(new DateTime(0))) match {
      case Nil => Nil
      case h :: t =>
        h.copy(
          yearlyFee = yearlyFee,
          actualAmountPayed = h.actualAmountPayed |+| debitor.extraPayments) :: t
    }
    val total = results.combineAll
    results :+ total
  }

  def compare(debitors: List[Debitor],
              payableAmounts: (BigDecimal, List[MonthlyFees]))
    : AnalyzerInterpreter.AmountsPerIntervalPerDebitor => Map[
      Debitor,
      List[ComparisonResult]] =
    (tans: AmountsPerIntervalPerDebitor) => {
      debitors
        .map(d => (d, compare(d, payableAmounts, tans.getOrElse(d, Map()))))
        .toMap
    }

  def toHtml(result: ComparisonResults): String = {
    val tables = result.map(format).toList.sorted.mkString("\n\n")
    val title =
      s"# Payments Scalaspatzen e.V."
    val md = s"""$title
       |
       |$tables
     """.stripMargin

    val options = new MutableDataSet()
    options.setFrom(ParserEmulationProfile.KRAMDOWN)
    options.set(
      Parser.EXTENSIONS,
      util.Arrays.asList(
        AbbreviationExtension.create(),
        DefinitionExtension.create(),
        FootnoteExtension.create(),
        TablesExtension.create(),
        TypographicExtension.create()
      )
    )

    val parser = Parser.builder(options).build()
    val renderer = HtmlRenderer.builder(options).build()

    val document = parser.parse(md)
    renderer.render(document)
  }

  private def format(
      resultsPerDebitor: (Debitor, List[ComparisonResult])): String = {
    val (debitor, results) = resultsPerDebitor
    val monthYearFormatter = DateTimeFormat.forPattern("yyyy MMMM")
    val tableHeader =
      s"| ${debitor.lastNames.sorted.mkString(", ")} | yearly | tuition | food | total | actual | diff |\n| --- | --- | --- | --- | --- | --- | --- |"
    val rows = results.map { result =>
      val interval = if (!result.isAggregate) {
        result.interval
          .map(i => monthYearFormatter.print(i.start))
          .getOrElse("-")
      } else {
        "sums"
      }
      val values = List(
        interval,
        result.yearlyFee.toString,
        result.tuition.toString,
        result.foodAllowance.toString,
        result.total.toString,
        result.actualAmountPayed.toString,
        result.diff.toString
      ).mkString(" | ")
      s"| $values |"
    }
    (tableHeader :: rows).mkString("\n")
  }
}

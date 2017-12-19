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
                         tuitionExpected,
                         foodExpected,
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

  def toHtml(result: ComparisonResults): String = {
    val md = toMarkdown(result)

    val options = new MutableDataSet()
      .setFrom(ParserEmulationProfile.KRAMDOWN)
      .set(TablesExtension.CLASS_NAME, "table")
      .set(
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
    val content = renderer.render(document)
    bootstrapTemplate(content)
  }

  private def toMarkdown(result: ComparisonResults) = {
    val tables =
      result.map(singleDebitorToMarkdown).toList.sorted.mkString("\n</br>\n")
    val title =
      s"# Payments Scalaspatzen e.V."
    s"""$title
       |
       |$tables
     """.stripMargin
  }

  private val monthYearFormatter = DateTimeFormat.forPattern("yyyy MMMM")

  private def singleDebitorToMarkdown(
      comparisonResults: (Debitor, List[ComparisonResult])): String = {
    val (debitor, results) = comparisonResults
    val tableHeader =
      s"| period | yearly (€) | tuition (€) | food (€) | total (€) | actual (€) | balance (€) |\n| --- | ---: | ---: | ---: | ---: | ---: | ---: |"
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
        result.yearlyFee.formatAmount,
        result.tuition.formatAmount,
        result.foodAllowance.formatAmount,
        result.total.formatAmount,
        result.actualAmountPayed.formatAmount,
        result.diff.formatAmount,
      ).mkString(" | ")
      s"| $values |"
    }
    val table = (tableHeader :: rows).mkString("\n")

    val paymentsHeader =
      "| period | payment date | amount (€) |\n| --- | --- | ---: |"
    val paymentsRows = results
      .filterNot(_.isAggregate)
      .flatMap(r => r.actualPayments.map(p => (r.interval, p)))
      .sortBy(_._2.date)
      .map(p =>
        s"| ${p._1.map(i => monthYearFormatter.print(i.start)).getOrElse("-")} | ${formatter
          .print(p._2.date)} | ${p._2.amount.formatAmount} |")
    val paymentsTable = (paymentsHeader :: paymentsRows).mkString("\n")

    s"""## ${debitor.lastNames.sorted.mkString(", ")} (${debitor.children
         .mkString(", ")})
       |
       |$table
       |
       |</br>
       |
       |$paymentsTable
       |
       |*additional payments*: ${results
         .filterNot(_.isAggregate)
         .foldMap(_.extraPayments)
         .formatAmount} €
     """.stripMargin
  }

  private def bootstrapTemplate(content: String) =
    s"""<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">

    <title>Payments Scalaspatzen e.V.</title>

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
  </head>

  <body>
    <div class="container">

      <div class="starter-template">
      $content
      </div>

    </div><!-- /.container -->


    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
  </body>
</html>"""
}

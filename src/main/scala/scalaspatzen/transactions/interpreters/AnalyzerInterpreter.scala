package scalaspatzen.transactions.interpreters

import java.util

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

import scala.util.Try

object AnalyzerInterpreter extends Analyzer[Debitor, Transaction, BigDecimal] {
  private def removeQuotes(str: String): String = {
    str.replace("\"", "")
  }

  private val formatter = DateTimeFormat.forPattern("dd.MM.yy")

  def decode(line: String): Option[Transaction] = {
    val elements = line.split("\";\"").map(removeQuotes)
    for {
      dateTime <- Try(formatter.parseDateTime(elements.head)).toOption
      amount <- Try(elements.drop(6).head.replace(".", "").replace(",", ".").replace("€", "").trim)
        .map(BigDecimal(_)).toOption
      tanType <- elements.drop(2).headOption.map(_ => if (amount >= 0) Credit else Debit)
      description <- elements.drop(3).headOption
      sender <- elements.drop(4).headOption
      receiver <- elements.drop(5).headOption
    } yield {
      Transaction(dateTime, tanType, description, sender, receiver, amount)
    }
  }

  def getAmount(tan: Transaction): BigDecimal = tan.amount

  def groupByDebitor(debitors: List[Debitor]): List[Transaction] => Map[Debitor, List[Transaction]] = (
    tans: List[Transaction]) => {
    tans
      .flatMap { tan =>
        debitors
          .find(d =>
            d.normalizedIdentifiers.exists(id =>
              tan.sender.toLowerCase.contains(id)))
          .map(d => (d, tan))
      }
      .groupBy(_._1)
      .mapValues(_.map(_._2))
  }

  private def getInterval(dt: DateTime): Interval = {
    val withCorrectMonth =
      if (dt.dayOfMonth().get <= 10)
        dt.plusMonths(-1)
      else
        dt
    val start = withCorrectMonth
      .withDayOfMonth(11)
      .withTimeAtStartOfDay()
    val end = withCorrectMonth
      .plusMonths(1)
      .withDayOfMonth(11)
      .withTimeAtStartOfDay()
      .plusSeconds(-1)
    start to end
  }

  def groupByTimeInterval: AnalyzerInterpreter.TransactionsPerDebitor => Map[Debitor, Map[Interval, List[Transaction]]] = (
    tansPerDebitor: TransactionsPerDebitor) =>
    tansPerDebitor.mapValues { v =>
      v.map(tan => (getInterval(tan.date), tan))
        .groupBy(_._1)
        .mapValues(_.map(_._2))
    }

  private def toMarkdown(tans: AmountPerIntervalPerDebitor) = {
    val fmt = DateTimeFormat.forPattern("dd.MM.yyy")
    val monthYearFormatter = DateTimeFormat.forPattern("MMMM yyyy")
    val intervals = tans.values.flatMap(_.keys).toList.distinct
    val start = intervals.minBy(_.startMillis).start
    val end = intervals.maxBy(_.endMillis).end
    val tableHeader =
      "| Name | Month | Amount |\n| --- | --- | --- |"
    val rows = tans.map {
      case (debitor, payments) =>
        val firstLine = s"| ${debitor.name} | | |"
        val subsequent = payments.map {
          case (interval, payment) =>
            s"| | ${monthYearFormatter.print(interval.end)} | $payment |"
        }.toList
        (firstLine :: subsequent).mkString("\n")
    }.toList

    val table = (tableHeader :: rows).mkString("\n")
    val title = s"# Payments Scalaspatzen e.V. ${fmt.print(start)} - ${fmt.print(end)}"
    List(title, table).mkString("\n\n")
  }

  def format(tans: AmountPerIntervalPerDebitor): RawLine = {
    val md = toMarkdown(tans)

    val options = new MutableDataSet()
    options.setFrom(ParserEmulationProfile.KRAMDOWN)
    options.set(
      Parser.EXTENSIONS, util.Arrays.asList(
        AbbreviationExtension.create(),
        DefinitionExtension.create(),
        FootnoteExtension.create(),
        TablesExtension.create(),
        TypographicExtension.create()
      ))

    val parser = Parser.builder(options).build()
    val renderer = HtmlRenderer.builder(options).build()

    val document = parser.parse(md)
    renderer.render(document)
  }

}

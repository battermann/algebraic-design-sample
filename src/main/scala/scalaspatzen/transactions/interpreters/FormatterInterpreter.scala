package scalaspatzen.transactions.interpreters

import java.util

import org.joda.time.format.DateTimeFormat
import com.github.nscala_time.time.Imports._

import scalaspatzen.transactions.algebra.Formatter
import scalaspatzen.transactions.model.{ComparisonResult, Debitor}
import cats.implicits._
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.typographic.TypographicExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.{Parser, ParserEmulationProfile}
import com.vladsch.flexmark.util.options.MutableDataSet

object FormatterInterpreter
    extends Formatter[Map[Debitor, List[ComparisonResult]]] {

  private val formatter = DateTimeFormat.forPattern("dd.MM.yy")

  def toMarkdown(results: Map[Debitor, List[ComparisonResult]]): String = {
    val tables =
      results.map(singleDebitorToMarkdown).toList.sorted.mkString("\n</br>\n")
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

  def markdownToHtml(md: String, css: String): String = {
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
    bootstrapTemplate(content, css)
  }

  private def bootstrapTemplate(content: String, css: String) = {
    s"""<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">
    <style type="text/css">
      $css
    </style>
    <title>Payments Scalaspatzen e.V.</title>
  </head>

  <body>
    <div class="container">

      <div class="starter-template">
      $content
      </div>

    </div><!-- /.container -->
  </body>
</html>"""
  }
}

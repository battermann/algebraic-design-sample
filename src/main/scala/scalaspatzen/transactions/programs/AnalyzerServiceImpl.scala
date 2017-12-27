package scalaspatzen.transactions.programs

import cats.{Monad, Monoid}
import cats.implicits._

import scala.language.higherKinds
import scalaspatzen.transactions.algebra._
import scalaspatzen.transactions.cli.{Html, OutputFormat, Pdf}

class AnalyzerServiceImpl[F[_]: Monad,
                          Debitor,
                          Payment,
                          Amount,
                          PayableAmounts,
                          ComparisonResult](
    fs: FileSystem[F],
    browser: Browser[F],
    analyzer: Analyzer[Debitor,
                       Payment,
                       Amount,
                       PayableAmounts,
                       ComparisonResult],
    resources: Resources[F, (List[Debitor], Int, PayableAmounts)],
    pdfConverter: PdfConverter[F, String],
    formatter: Formatter[Map[Debitor, List[ComparisonResult]]])(
    implicit m: Monoid[BigDecimal])
    extends AnalyzerService[F, Map[Debitor, List[ComparisonResult]]] {

  def generateHtmlReport(
      inputDir: String): F[Map[Debitor, List[ComparisonResult]]] = {
    import analyzer._
    import fs._
    import resources._

    def analyze(
        debitors: List[Debitor],
        paymentsDueDayOfMonth: Int,
        payableAmounts: PayableAmounts): List[RawLine] => ComparisonResults =
      decodeLines andThen
        groupByDebitor(debitors) andThen
        groupByTimeInterval(paymentsDueDayOfMonth) andThen
        compare(debitors, payableAmounts)

    for {
      files <- listFiles(inputDir)
      csvFiles = files.filter(path => path.endsWith(".csv"))
      rawLines <- csvFiles.traverse(readAllLines("Windows-1250"))
      env <- getConfig
    } yield {
      val (debitors, paymentsDueDayOfMonth, payableAmounts) = env
      analyze(debitors, paymentsDueDayOfMonth, payableAmounts)(rawLines.flatten)
    }
  }

  def saveHtmlReport(report: String, filename: String): F[Unit] = {
    fs.writeAllText(report, filename)
  }

  def openReportInBrowser(filename: String): F[Unit] = {
    browser.openFile(filename)
  }

  def generateAndOpenReport(input: String,
                            output: String,
                            outputFormat: OutputFormat): F[Unit] = {
    import formatter._
    import resources._

    for {
      report <- generateHtmlReport(input)
      md = toMarkdown(report)
      css <- getCss
      html = markdownToHtml(md, css)
      _ <- saveHtmlReport(html, s"$output.html")
      file <- outputFormat match {
        case Html => s"$output.html".pure[F]
        case Pdf =>
          val filename = s"$output.pdf"
          exportToPdf(html, filename).map(_ => filename)
      }
      _ <- openReportInBrowser(file)
    } yield ()
  }

  def exportToPdf(html: String, filename: String): F[Unit] = {
    pdfConverter.exportToPdf(html, filename)
  }
}

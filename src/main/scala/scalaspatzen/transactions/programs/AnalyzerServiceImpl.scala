package scalaspatzen.transactions.programs

import cats.Monoid
import cats.implicits._

import scalaspatzen.transactions.algebra._
import scalaspatzen.transactions.interpreters.ErrorOrIO
import scalaspatzen.transactions.model._

class AnalyzerServiceImpl(
    fs: FileSystem[ErrorOrIO],
    browser: Browser[ErrorOrIO],
    analyzer: Analyzer[Debitor,
                       Payment,
                       BigDecimal,
                       (BigDecimal, List[MonthlyFees]),
                       ComparisonResult],
    config: Config[ErrorOrIO, Environment])(implicit m: Monoid[BigDecimal])
    extends AnalyzerService[ErrorOrIO, String] {

  def generateReport(inputDir: String): ErrorOrIO[String] = {
    import analyzer._
    import config._
    import fs._

    def sumAmounts
      : TransactionsPerIntervalPerDebitor => AmountsPerIntervalPerDebitor =
      _.mapValues(x => x.mapValues(_.map(getAmount).combineAll))

    def analyze(debitors: List[Debitor],
                paymentsDueDayOfMonth: Int,
                payableAmounts: (BigDecimal, List[MonthlyFees]))
      : List[RawLine] => ComparisonResults =
      decodeLines andThen
        groupByDebitor(debitors) andThen
        groupByTimeInterval(paymentsDueDayOfMonth) andThen
        sumAmounts andThen
        compare(debitors, payableAmounts)

    for {
      files    <- listFiles(inputDir)
      csvFiles = files.filter(path => path.endsWith(".csv"))
      rawLines <- csvFiles.traverse(readAllLines("Windows-1250"))
      c        <- getConfig
      result   = analyze(c.debitors, c.paymentsDueDayOfMonth, (c.yearlyFee, c.monthlyFees))(rawLines.flatten)
    } yield toHtml(result)
  }

  def saveReport(report: String, outputPath: String): ErrorOrIO[Unit] = {
    fs.writeAllText(report, outputPath)
  }

  def openReportInBrowser(filePath: String): ErrorOrIO[Unit] = {
    browser.openFile(filePath)
  }

  def generateReportAndOpenInBrowser(input: String, output: String): ErrorOrIO[Unit] = {
    for {
      report <- generateReport(input)
      _ <- saveReport(report, output)
      _ <- openReportInBrowser(output)
    } yield ()
  }
}

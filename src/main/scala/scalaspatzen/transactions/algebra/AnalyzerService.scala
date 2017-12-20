package scalaspatzen.transactions.algebra

import scala.language.higherKinds

trait AnalyzerService[F[_], Report] {
  def generateHtmlReport(inputDir: String): F[Report]
  def exportToPdf(html: String, filename: String): F[Unit]
  def saveHtmlReport(report: String, outputPath: String): F[Unit]
  def openReportInBrowser(filename: String): F[Unit]
}

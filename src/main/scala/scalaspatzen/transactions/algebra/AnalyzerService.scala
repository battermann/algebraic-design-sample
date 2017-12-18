package scalaspatzen.transactions.algebra

import scala.language.higherKinds

trait AnalyzerService[F[_], Report] {
  def generateReport(inputDir: String): F[Report]
  def saveReport(report: Report, outputPath: String): F[Unit]
  def openReportInBrowser(filePath: String): F[Unit]
}

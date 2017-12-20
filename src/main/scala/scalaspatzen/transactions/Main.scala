package scalaspatzen.transactions

import caseapp.{CaseApp, RemainingArgs}
import cats.implicits._

import scalaspatzen.transactions.interpreters._
import scalaspatzen.transactions.programs.AnalyzerServiceImpl
import CliOptions._

object Main extends CaseApp[CliOptions] {
  override def run(options: CliOptions, remainingArgs: RemainingArgs): Unit = {

    val programs = new AnalyzerServiceImpl(FileSystemInterpreter,
                                           BrowserInterpreter,
                                           AnalyzerInterpreter,
                                           ResourcesInterpreter$,
                                           PdfConverterInterpreter,
                                           FormatterInterpreter)

    import programs._

    generateAndOpenReport(options.input, options.output, options.outputFormat).value
      .unsafeRunSync()
  }
}

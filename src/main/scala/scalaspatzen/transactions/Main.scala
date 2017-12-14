package scalaspatzen.transactions

import scalaspatzen.transactions.interpreters.{AnalyzerInterpreter, BrowserInterpreter, ConfigInterpreter, FileSystemInterpreter}
import scalaspatzen.transactions.programs.Programs
import caseapp.{CaseApp, RemainingArgs}
import cats.implicits._

object Main extends CaseApp[CliOptions] {
  override def run(options: CliOptions, remainingArgs: RemainingArgs): Unit = {

    val analyzer = Programs.generateAndOpenPaymentReport(FileSystemInterpreter,
                                    AnalyzerInterpreter,
                                    ConfigInterpreter,
                                    BrowserInterpreter) _

    analyzer(options.input, options.output).value.unsafeRunSync()
  }
}

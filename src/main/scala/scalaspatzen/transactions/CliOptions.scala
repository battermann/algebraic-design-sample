package scalaspatzen.transactions

import caseapp.core.Error.UnrecognizedValue
import caseapp.core.argparser.{ArgParser, SimpleArgParser}
import caseapp.{ExtraName, HelpMessage, ValueDescription}

import scalaspatzen.transactions.model.{Html, OutputFormat, Pdf}

case class CliOptions(
  @HelpMessage("The relative or absolute path to the directory containing the .csv the files.")
  @ValueDescription("string")
  @ExtraName("i")
  input: String,
  @HelpMessage("The name for the generated report files. Omit file extension. (e.g. -o payment-report)")
  @ValueDescription("string")
  @ExtraName("o")
  output: String,
  @HelpMessage("Supported output formats: html (default), pdf.")
  @ValueDescription("html | pdf")
  @ExtraName("f")
  outputFormat: OutputFormat = Html
)

object CliOptions {
  implicit val customParallelismParser: ArgParser[OutputFormat] =
    SimpleArgParser.from[OutputFormat]("custom") {
      case "html" => Right(Html)
      case "pdf" => Right(Pdf)
      case x => Left(UnrecognizedValue(x))
    }
}

package scalaspatzen.transactions

import caseapp.{ExtraName, HelpMessage, ValueDescription}

case class CliOptions(
  @HelpMessage("The relative or absolute path to the directory containing the .csv the files.")
  @ValueDescription("string")
  @ExtraName("i")
  input: String,
  @HelpMessage("The name for the generated report files. Please omit file extension. (e.g. -o payment-report)")
  @ValueDescription("string")
  @ExtraName("o")
  output: String
)

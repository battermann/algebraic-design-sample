package scalaspatzen.transactions

import caseapp.ExtraName

case class CliOptions(
  @ExtraName("i")
  input: String,
  @ExtraName("o")
  output: String
)

package scalaspatzen.transactions.cli

sealed trait OutputFormat
case object Html extends OutputFormat
case object Pdf extends OutputFormat

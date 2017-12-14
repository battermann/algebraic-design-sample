package scalaspatzen.transactions.programs

import scalaspatzen.transactions.algebra._
import cats._
import cats.implicits._

import scala.language.higherKinds

object Programs {
  def generateAndOpenPaymentReport[F[_]: Monad, Debitor, Transaction, Amount](
      fileSystem: FileSystem[F],
      analyzer: Analyzer[Debitor, Transaction, Amount],
      config: Config[F, Debitor],
      browser: Browser[F])(directory: String, output: String)(
      implicit amountMonoid: Monoid[Amount]): F[Unit] = {

    import analyzer._
    import fileSystem._

    def analyze(debitors: List[Debitor]): List[RawLine] => String =
      decodeLines andThen
        groupByDebitor(debitors) andThen
        groupByTimeInterval andThen
        sumAmounts andThen
        format

    for {
      files <- listFiles(directory)
      csvFiles = files.filter(path => path.endsWith(".csv"))
      rawLines <- csvFiles.traverse(readAllLines("Windows-1250"))
      debitors <- config.debitors
      result = analyze(debitors)(rawLines.flatten)
      path <- fileSystem.writeAllText(result, output)
      _ <- browser.openFile(path)
    } yield ()
  }
}

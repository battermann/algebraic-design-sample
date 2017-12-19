package scalaspatzen.transactions.algebra

import scala.language.higherKinds

trait Browser[F[_]] {
  def openFile(filename: String): F[Unit]
}

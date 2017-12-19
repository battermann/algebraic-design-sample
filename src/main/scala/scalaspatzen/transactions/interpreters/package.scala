package scalaspatzen.transactions

import cats.data.EitherT
import cats.effect.IO

package object interpreters {
  type ErrorOrIO[A] = EitherT[IO, Throwable, A]

  implicit class LiftIO[A](x: IO[A]) {
    def liftIO: ErrorOrIO[A] = EitherT.liftF(x)
  }

  implicit class AmountFormatter(amount: BigDecimal) {
    def formatAmount: String = f"$amount%1.2f"
  }
}

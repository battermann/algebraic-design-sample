package scalaspatzen.transactions.algebra

import scala.language.higherKinds

trait Config[F[_], Debitor] {
  val debitors: F[List[Debitor]]
}

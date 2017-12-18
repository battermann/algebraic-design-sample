package scalaspatzen.transactions.algebra

import scala.language.higherKinds

trait Config[F[_], Config] {
  val getConfig: F[Config]
}

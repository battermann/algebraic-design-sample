package scalaspatzen.transactions.algebra

import scala.language.higherKinds

trait Resources[F[_], Config] {
  val getConfig: F[Config]
  val getCss: F[String]
}

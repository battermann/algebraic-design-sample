package scalaspatzen.transactions.algebra

import scala.language.higherKinds

trait FileSystem[F[_]] {
  def listFiles(directory: String): F[List[String]]
  def readAllLines(enc: String)(fileName: String): F[List[String]]
  def writeAllText(text: String, fileName: String): F[Unit]
}
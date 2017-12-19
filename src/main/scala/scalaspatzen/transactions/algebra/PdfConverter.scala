package scalaspatzen.transactions.algebra

import scala.language.higherKinds

trait PdfConverter[F[_], Html] {
  def exportToPdf(html: Html, fileName: String): F[Unit]
}

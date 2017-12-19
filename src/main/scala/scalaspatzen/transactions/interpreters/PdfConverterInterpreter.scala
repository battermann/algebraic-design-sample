package scalaspatzen.transactions.interpreters

import java.io.FileOutputStream

import cats.data.EitherT
import cats.effect.IO
import com.openhtmltopdf.DOMBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup

import scalaspatzen.transactions.algebra.PdfConverter

object PdfConverterInterpreter extends PdfConverter[ErrorOrIO, String] {
  def exportToPdf(html: String, fileName: String): ErrorOrIO[Unit] = EitherT {
    IO {
      val doc =  DOMBuilder.jsoup2DOM(Jsoup.parse(html))
      val os = new FileOutputStream(fileName)
      val builder = new PdfRendererBuilder
      builder.withW3cDocument(doc, "")
      builder.toStream(os)
      builder.run()
    }.attempt
  }
}

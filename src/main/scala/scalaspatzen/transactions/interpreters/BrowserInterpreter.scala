package scalaspatzen.transactions.interpreters

import java.awt.Desktop
import java.net.URI

import scalaspatzen.transactions.algebra.Browser
import cats.data.EitherT
import cats.effect.IO

object BrowserInterpreter extends Browser[ErrorOrIO] {

  def openFile(filePath: String) = {
    EitherT {
      IO {
        Desktop.getDesktop.browse(new URI("file://" + filePath))
      }.attempt
    }
  }
}

package scalaspatzen.transactions.interpreters

import java.io.{File, PrintWriter}

import scalaspatzen.transactions.algebra.FileSystem
import cats.data.EitherT
import cats.effect.IO

object FileSystemInterpreter extends FileSystem[ErrorOrIO] {
  def listFiles(directory: String) =
    EitherT {
      IO {
        val dir = new File(directory)
        dir.listFiles().filter(_.isFile).map(_.getAbsolutePath).toList
      }.attempt
    }

  def readAllLines(enc: String)(fileName: String) =
    EitherT {
      IO {
        scala.io.Source.fromFile(fileName, enc).getLines.toList
      }.attempt
    }

  def writeAllText(text: String, fileName: String) =
    EitherT {
      IO {
        val writer = new PrintWriter(new File(fileName))
        writer.write(text)
        writer.close()
      }.attempt
    }
}

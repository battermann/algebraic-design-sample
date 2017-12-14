package scalaspatzen.transactions.interpreters

import scalaspatzen.transactions.algebra.Config
import scalaspatzen.transactions.model.Debitor
import cats.effect.IO

object ConfigInterpreter extends Config[ErrorOrIO, Debitor] {
  val debitors: ErrorOrIO[List[Debitor]] = IO { debitorList }.liftIO

  private val debitorList = List(
    Debitor(child = "Robert Probst",
            name = "Ulrike und Tobias Probst",
            identifiers = List("Probst")),
    Debitor(child = "Erik Austerlitz",
            name = "Leonie und Ralf Austerlitz",
            identifiers = List("Austerlitz")),
    Debitor(child = "Sarah Kästner",
            name = "Daniel Becker und Marina Kästner",
            identifiers = List("Kästner", "Becker")),
  )
}

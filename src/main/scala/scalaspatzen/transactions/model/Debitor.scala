package scalaspatzen.transactions.model

import java.text.Normalizer

final case class Debitor(
  name: String,
  child: String,
  identifiers: List[String] = Nil
) {
  val normalizedIdentifiers: List[String] = {
    identifiers.map(s => Normalizer
      .normalize(s, Normalizer.Form.NFD)
      .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
      .toLowerCase
    )
  }
}
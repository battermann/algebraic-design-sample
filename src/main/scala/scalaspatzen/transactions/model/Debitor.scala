package scalaspatzen.transactions.model

import java.text.Normalizer

import org.joda.time.Interval

final case class Debitor(
    lastNames: List[String],
    children: List[String],
    tuitionSuspended: List[Interval],
    foodAllowanceSuspended: List[Interval],
    extraPayments: BigDecimal
) {
  val normalizedSenderIds: List[String] = {
    (lastNames ++ lastNames.map(
      _.replaceAll("[äÄ]", "ae").replaceAll("[öÜ]", "oe").replaceAll("[üÜ]", "ue"))).distinct.map(
      s =>
        Normalizer
          .normalize(s, Normalizer.Form.NFD)
          .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
          .toLowerCase)
  }
}

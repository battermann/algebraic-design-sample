package scalaspatzen.transactions.model

import java.text.Normalizer

import org.joda.time.Interval

final case class Debitor(
  lastNames: List[String],
  children: List[String],
  senderIds: List[String],
  tuitionSuspended: List[Interval],
  foodAllowanceSuspended: List[Interval],
  extraPayments: BigDecimal
) {
  val normalizedSenderIds: List[String] = {
    senderIds.map(s => Normalizer
      .normalize(s, Normalizer.Form.NFD)
      .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
      .toLowerCase
    )
  }
}
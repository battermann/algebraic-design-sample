package scalaspatzen.transactions.interpreters

import cats.data.EitherT
import cats.effect.IO
import com.github.nscala_time.time.Imports._
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import jsonmodels.environmentconfig.EnvironmentConfig
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json

import scalaspatzen.transactions.algebra.Resources
import scalaspatzen.transactions.model.{Debitor, Environment, MonthlyFees}

object ResourcesInterpreter$ extends Resources[ErrorOrIO, Environment] {
  private val config = ConfigFactory.load()
  private val formatter = DateTimeFormat.forPattern("MM.yyyy")
  private def toInterval(dateString: String) = {
    val start = formatter.parseDateTime(dateString)
    start to start.plusMonths(1)
  }
  val getConfig: ErrorOrIO[Environment] = EitherT {
    IO {
      val configJsonString = config.root().render(ConfigRenderOptions.concise())
      val envConf = Json.parse(configJsonString).as[EnvironmentConfig]
      Environment(
        paymentsDueDayOfMonth = envConf.paymentsDueDayOfMonth.toInt,
        yearlyFee = BigDecimal(envConf.yearlyFee.toString),
        monthlyFees = envConf.monthlyFees
          .map{
            mf =>
              MonthlyFees(toInterval(mf.month),
                          BigDecimal(mf.tuition.toString),
                          BigDecimal(mf.foodAllowance.toString))}
          .toList,
        debitors = envConf.debitors
          .map(d =>
            Debitor(
              lastNames = d.lastNames.toList,
              children = d.children.toList,
              tuitionSuspended =
                d.tuitionSuspended.map(_.map(toInterval)).toList.flatten,
              foodAllowanceSuspended =
                d.foodAllowanceSuspended.map(_.map(toInterval)).toList.flatten,
              extraPayments = BigDecimal(d.extraPayments.toString)
          ))
          .toList
      )
    }.attempt
  }

  val getCss: ErrorOrIO[String] = EitherT {
    IO {
      io.Source.fromURL(getClass.getResource("/bootstrap.min.css")).mkString
    }.attempt
  }
}

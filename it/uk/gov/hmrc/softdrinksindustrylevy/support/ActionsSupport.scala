package uk.gov.hmrc.softdrinksindustrylevy.support

import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status

import scala.concurrent.ExecutionContext

trait ActionsSupport extends ScalaFutures with Status with Matchers {

  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  implicit val ec = ExecutionContext.global

}

package uk.gov.hmrc.softdrinksindustrylevy.support

import java.util.UUID

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest._
import play.api.http.Status

trait IntegrationSpec extends FlatSpec
  with GivenWhenThen
  with ScalaFutures
  with Matchers
  with Status
  with Eventually
  with BeforeAndAfterEach {

}

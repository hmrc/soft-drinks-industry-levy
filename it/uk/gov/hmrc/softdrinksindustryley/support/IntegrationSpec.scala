package uk.gov.hmrc.softdrinksindustryley.support

import java.util.UUID

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen, Matchers}
import play.api.http.Status

trait IntegrationSpec extends FeatureSpec
  with GivenWhenThen
  with ScalaFutures
  with Matchers
  with Status
  with Eventually
  with BeforeAndAfterEach {

}

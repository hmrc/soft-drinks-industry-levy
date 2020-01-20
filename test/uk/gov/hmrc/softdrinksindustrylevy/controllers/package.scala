/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.softdrinksindustrylevy

import java.time.{LocalDate, LocalDateTime}

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.softdrinksindustrylevy.models.ActivityType.{Copackee, CopackerAll, Imported, ProducedOwnBrand}
import uk.gov.hmrc.softdrinksindustrylevy.models._

package object controllers {

  val formBundleNumber = "asdfasdfsadf"

  val validCreateSubscriptionRequest: JsValue = Json.parse("""{
                                                             |  "utr" : "7674173564",
                                                             |  "sdilRef" : "XASDIL000999990",
                                                             |  "orgName" : "fgdiukxkTwyrorGj",
                                                             |  "orgType" : "1",
                                                             |  "address" : {
                                                             |    "postCode" : "ZE13 8JG",
                                                             |    "lines" : ["137 Crooked S Road", "Lerwick"]
                                                             |  },
                                                             |  "activity" : {
                                                             |    "CopackerAll" : {
                                                             |      "lower" : 677551,
                                                             |      "upper" : 491823
                                                             |    },
                                                             |    "isLarge" : true
                                                             |  },
                                                             |  "liabilityDate" : "1977-07-08",
                                                             |  "productionSites" : [ {
                                                             |    "address" : {
                                                             |      "postCode" : "PL39 9DF",
                                                             |      "lines" : ["6 Lageonan Road", "Plymouth"]
                                                             |    },
                                                             |    "ref" : "46f9cafe-d7bd-49c0-9c46-64ee5505fdb2"
                                                             |  }, {
                                                             |    "address" : {
                                                             |      "postCode" : "DY96 0BX",
                                                             |      "lines" : ["92 The Commons", "Dudley"]
                                                             |    },
                                                             |    "ref" : "d594e118-63ac-49eb-a48e-1a8d9e377533"
                                                             |  }],
                                                             |  "warehouseSites" : [ {
                                                             |    "address" : {
                                                             |      "postCode" : "EX40 4WB",
                                                             |      "lines" : ["45 Wine Street", "Exeter"]
                                                             |    },
                                                             |    "ref" : "01972ba0-4cd3-4be4-9a97-3c1a727d54a5"
                                                             |  }, {
                                                             |    "address" : {
                                                             |      "postCode" : "WD06 1ZL",
                                                             |      "lines" : ["65 High Cedar Drive", "Watford"]
                                                             |    },
                                                             |    "ref" : "8a9e17f9-6d93-44b2-a2f0-9dab3781f1b6"
                                                             |  } ],
                                                             |  "contact" : {
                                                             |    "name" : "Alexander",
                                                             |    "positionInCompany" : "Pearson",
                                                             |    "phoneNumber" : "Yfsdpygfvv",
                                                             |    "email" : "kmxjmvuhyJcndpkLhcdeqgv@Cxb.co.uk"
                                                             |  }
                                                             |}
                                                             |""".stripMargin)

  val validSubscriptionResponse = CreateSubscriptionResponse(
    LocalDateTime.parse("2017-11-13T13:48:21.81"),
    "814892841918"
  )

  val validRosmRegisterResponse = RosmRegisterResponse(
    "fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
    None,
    false,
    false,
    false,
    Some(OrganisationResponse("foo")),
    None,
    RosmResponseAddress(
      "50",
      Some("The Lane"),
      Some("vclmtrtcivhcjldlfeysrttlpfykeolmkpcikccignlvyvghp"),
      None,
      "GB",
      "SM32 5IA"),
    RosmResponseContactDetails(
      Some("08926 167394"),
      None,
      None,
      Some("qovmlk@rlkioorw.com")
    )
  )

  val sub = Subscription(
    "1234567890",
    Some("1234"),
    "org name",
    None,
    UkAddress(List("line1"), "AA11AA"),
    activity,
    LocalDate.now(),
    List(Site(UkAddress(List("line1"), "AA11AA"), None, None, None)),
    List(Site(UkAddress(List("line1"), "AA11AA"), None, None, None)),
    Contact(None, None, "0843858438", "test@test.com"),
    None,
    None
  )

  def internalActivity(
    produced: LitreBands = zero,
    copackedAll: LitreBands = zero,
    imported: LitreBands = zero,
    copackedByOthers: LitreBands = zero) =
    InternalActivity(
      Map(
        ProducedOwnBrand -> produced,
        CopackerAll      -> copackedAll,
        Imported         -> imported,
        Copackee         -> copackedByOthers
      ),
      false
    )

  lazy val zero: LitreBands = (0, 0)

  lazy val activity = internalActivity(
    produced = (1, 2),
    copackedAll = (3, 4),
    imported = (5, 6)
  )

}

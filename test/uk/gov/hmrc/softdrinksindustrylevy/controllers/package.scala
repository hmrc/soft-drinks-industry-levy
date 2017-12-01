/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.LocalDateTime

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.softdrinksindustrylevy.models.CreateSubscriptionResponse

package object controllers {

  val invalidCreateSubscriptionRequest: JsValue = Json.parse(
"""{
  |"test": "bad"
  |}
""".stripMargin
  )

  val validCreateSubscriptionRequest: JsValue = Json.parse(
    """{
      |  "utr" : "7674173564",
      |  "orgName" : "fgdiukxkTwyrorGj",
      |  "address" : {
      |    "notUKAddress" : false,
      |    "postCode" : "ZE13 8JG",
      |    "line1" : "137 Crooked S Road",
      |    "line2" : "Lerwick"
      |  },
      |  "activity" : {
      |    "CopackerAll" : {
      |      "lower" : 677551,
      |      "upper" : 491823
      |    },
      |    "CopackerSmall" : {
      |      "lower" : 272828,
      |      "upper" : 168877
      |    }
      |  },
      |  "liabilityDate" : "1977-07-08",
      |  "productionSites" : [ {
      |    "address" : {
      |      "notUKAddress" : false,
      |      "postCode" : "PL39 9DF",
      |      "line1" : "6 Lageonan Road",
      |      "line2" : "Plymouth"
      |    },
      |    "ref" : "46f9cafe-d7bd-49c0-9c46-64ee5505fdb2"
      |  }, {
      |    "address" : {
      |      "notUKAddress" : false,
      |      "postCode" : "DY96 0BX",
      |      "line1" : "92 The Commons",
      |      "line2" : "Dudley"
      |    },
      |    "ref" : "d594e118-63ac-49eb-a48e-1a8d9e377533"
      |  }],
      |  "warehouseSites" : [ {
      |    "address" : {
      |      "notUKAddress" : false,
      |      "postCode" : "EX40 4WB",
      |      "line1" : "45 Wine Street",
      |      "line2" : "Exeter"
      |    },
      |    "ref" : "01972ba0-4cd3-4be4-9a97-3c1a727d54a5"
      |  }, {
      |    "address" : {
      |      "notUKAddress" : false,
      |      "postCode" : "WD06 1ZL",
      |      "line1" : "65 High Cedar Drive",
      |      "line2" : "Watford"
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

}

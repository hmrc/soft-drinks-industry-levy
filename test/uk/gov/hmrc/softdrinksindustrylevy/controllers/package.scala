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
      |    "registration": {
      |        "organisationType": "1",
      |        "applicationDate": "1920-02-29",
      |        "taxStartDate": "1920-02-29",
      |        "cin": "111111111111111",
      |        "tradingName": "a",
      |        "businessContact": {
      |            "addressDetails": {
      |                "notUKAddress": false,
      |                "line1": "Juicey Juices",
      |                "line2": "Some Street",
      |                "line3": " ",
      |                "line4": " ",
      |                "postCode": "AB012AA",
      |                "country": "GB"
      |            },
      |            "contactDetails": {
      |                "telephone": "01234567890",
      |                "mobile": "07890123456",
      |                "fax": "01234567111",
      |                "email": "a.b@c.com"
      |            }
      |        },
      |        "correspondenceContact": {
      |            "addressDetails": {
      |                "notUKAddress": false,
      |                "line1": "Juicey Juices",
      |                "line2": "Someother Street",
      |                "line3": " ",
      |                "line4": "Somewhere Else",
      |                "postCode": "AB012CC",
      |                "country": "GB"
      |            },
      |            "contactDetails": {
      |                "telephone": " ",
      |                "mobile": " ",
      |                "email": "a.b@c.com"
      |            },
      |            "differentAddress": true
      |        },
      |        "primaryPersonContact": {
      |            "name": "a",
      |            "positionInCompany": "a",
      |            "telephone": "(+44",
      |            "mobile": "a",
      |            "email": "a.b@c.com"
      |        },
      |        "details": {
      |            "producer": true,
      |            "producerDetails": {
      |                "produceMillionLitres": true,
      |                "producerClassification": "1",
      |                "smallProducerExemption": true,
      |                "useContractPacker": true,
      |                "voluntarilyRegistered": true
      |            },
      |            "importer": true,
      |            "contractPacker": true
      |        },
      |        "activityQuestions": {
      |            "litresProducedUKHigher": 2,
      |            "litresProducedUKLower": 2,
      |            "litresImportedUKHigher": 2,
      |            "litresImportedUKLower": 2,
      |            "litresPackagedUKHigher": 2,
      |            "litresPackagedUKLower": 2
      |        },
      |        "estimatedTaxAmount": 0.02,
      |        "taxObligationStartDate": "1920-02-29"
      |    },
      |    "sites": [
      |        {
      |            "action": "1",
      |            "tradingName": "a",
      |            "newSiteRef": "a",
      |            "siteAddress": {
      |                "addressDetails": {
      |                    "notUKAddress": true,
      |                    "line1": " ",
      |                    "line2": " ",
      |                    "line3": " ",
      |                    "line4": " ",
      |                    "postCode": "A00AA",
      |                    "country": "FR"
      |                },
      |                "contactDetails": {
      |                    "telephone": " ",
      |                    "mobile": " ",
      |                    "email": "a.b@c.com"
      |                }
      |            },
      |            "siteType": "2"
      |        },
      |        {
      |            "action": "1",
      |            "tradingName": "a",
      |            "newSiteRef": "a",
      |            "siteAddress": {
      |                "addressDetails": {
      |                    "notUKAddress": true,
      |                    "line1": " ",
      |                    "line2": " ",
      |                    "line3": " ",
      |                    "line4": " ",
      |                    "postCode": "A00AA",
      |                    "country": "DE"
      |                },
      |                "contactDetails": {
      |                    "telephone": " ",
      |                    "mobile": " ",
      |                    "email": "a.b@c.com"
      |                }
      |            },
      |            "siteType": "2"
      |        }
      |    ],
      |    "entityAction": [
      |        {
      |            "action": "1",
      |            "entityType": "4",
      |            "organisationType": "1",
      |            "cin": "a",
      |            "tradingName": "a",
      |            "businessContact": {
      |                "addressDetails": {
      |                    "notUKAddress": false,
      |                    "line1": " ",
      |                    "line2": " ",
      |                    "line3": " ",
      |                    "line4": " ",
      |                    "postCode": "A00AA",
      |                    "country": "GB"
      |                },
      |                "contactDetails": {
      |                    "telephone": " ",
      |                    "mobile": " ",
      |                    "email": "a.b@c.com"
      |                }
      |            }
      |        },
      |        {
      |            "action": "1",
      |            "entityType": "4",
      |            "organisationType": "1",
      |            "cin": "a",
      |            "tradingName": "a",
      |            "businessContact": {
      |                "addressDetails": {
      |                    "notUKAddress": false,
      |                    "line1": " ",
      |                    "line2": " ",
      |                    "line3": " ",
      |                    "line4": " ",
      |                    "postCode": "A00AA",
      |                    "country": "GB"
      |                },
      |                "contactDetails": {
      |                    "telephone": " ",
      |                    "mobile": " ",
      |                    "email": "a.b@c.com"
      |                }
      |            }
      |        }
      |    ]
      |}""".stripMargin)

  val validSubscriptionResponse = CreateSubscriptionResponse(LocalDateTime.parse("2017-11-13T13:48:21.81"), "814892841918")


}

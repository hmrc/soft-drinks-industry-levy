/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.softdrinksindustrylevy.models._

package object controllers {

  val formBundleNumber = "asdfasdfsadf"

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
      |    "postCode" : "ZE13 8JG",
      |    "lines" : ["137 Crooked S Road", "Lerwick"]
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

  val validSubscriptionRetrieve: JsValue =  Json.parse(
    """{
      |	"safeId": "XA0000000000000",
      |	"nino": "AA000000A",
      |	"utr": "9876543210",
      |	"subscriptionDetails": {
      |		"sdilRegistrationNumber": "a",
      |		"taxObligationStartDate": "2017-07-29",
      |		"taxObligationEndDate": "2017-10-29",
      |		"tradingName": "a",
      |		"voluntaryRegistration": false,
      |		"smallProducer": false,
      |		"largeProducer": true,
      |		"contractPacker": true,
      |		"importer": true,
      |		"primaryContactName": "Joe Bloggs",
      |		"primaryPositionInCompany": "Master Brewer",
      |		"primaryTelephone": "01234567890",
      |		"primaryMobile": "07777666555",
      |		"primaryEmail": "a@bcd.com"
      |	},
      |	"businessAddress": {
      |		"line1": "1 Here",
      |		"line2": "There",
      |		"line3": "Everywhere",
      |		"postCode": "SK12AB",
      |		"country": "AD"
      |	},
      |	"businessContact": {
      |		"telephone": "01234567890",
      |		"mobile": "07777666444",
      |		"email": "a@bcd.com"
      |	},
      |	"correspondenceAddress": {
      |		"line1": "2 Here",
      |		"line2": "There",
      |		"line3": "Overthere",
      |		"line4": "Near Here",
      |		"postCode": "A00AA",
      |		"country": "AD"
      |	},
      |	"correspondenceContact": {
      |		"telephone": "09876543210",
      |		"mobile": "07654321098",
      |		"fax": "09876543211",
      |		"email": "a@bcd.com"
      |	},
      |	"sites": [
      |		{
      |			"siteReference": "a",
      |			"tradingName": "Some Company",
      |			"siteAddress": {
      |				"line1": "1 Some Street",
      |				"line2": "Some Town",
      |				"line3": "Some District",
      |				"line4": "Shropshire",
      |				"postCode": "TF37RT",
      |				"country": "GB"
      |			},
      |			"siteContact": {
      |				"telephone": "01234567890",
      |				"mobile": "07891234567",
      |				"fax": "01234567891",
      |				"email": "a@bcd.com"
      |			},
      |			"siteType": "1"
      |		},
      |		{
      |			"siteReference": "b",
      |			"tradingName": "Some Other Company",
      |			"siteAddress": {
      |				"line1": "1 Some Street",
      |				"line2": "Some Town",
      |				"postCode": "TF34NT",
      |				"country": "GB"
      |			},
      |			"siteContact": {
      |				"telephone": "01234567890",
      |				"mobile": "07891234567",
      |				"email": "a@bcd.com"
      |			},
      |			"siteType": "2"
      |		}
      |	]
      |}""".stripMargin)

  val validRosmRegisterResponse = RosmRegisterResponse(
    "fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
    None,
    false,
    false,
    false,
    Some(OrganisationResponse("foo")),
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

}

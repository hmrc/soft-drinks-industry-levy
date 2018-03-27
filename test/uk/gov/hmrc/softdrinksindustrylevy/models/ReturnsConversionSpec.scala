package uk.gov.hmrc.softdrinksindustrylevy.models

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json
import uk.gov.hmrc.softdrinksindustrylevy.models.json.des.returns._

class ReturnsConversionSpec extends FunSuite with PropertyChecks with Matchers {

  test("∀ Returns: toJson(x) is valid") {
    val validator = JsonSchemaFactory.byDefault.getValidator

    val stream = getClass.getResourceAsStream(
      "/test/des-return.schema.json")
    val schemaText = scala.io.Source.fromInputStream(stream).getLines.mkString
    stream.close
    val schema = JsonLoader.fromString(schemaText)
    forAll { r: ReturnsRequest =>
      val json = JsonLoader.fromString(Json.prettyPrint(Json.toJson(r)))
      val report = validator.validate(schema, json)
      assert(report.isSuccess, report)
    }
  }

}

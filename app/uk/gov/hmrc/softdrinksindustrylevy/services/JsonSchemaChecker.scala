/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.softdrinksindustrylevy.services

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.{Schema, SchemaRegistry, SpecificationVersion}
import play.api.Logger
import play.api.libs.json.{Format, Json}

import scala.io.Source
import scala.jdk.CollectionConverters._

object JsonSchemaChecker {

  lazy val logger: Logger = Logger(this.getClass)

  private val objectMapper: ObjectMapper = new ObjectMapper()

  private val schemaRegistry: SchemaRegistry =
    SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4)

  def retrieveSchema(file: String): Schema =
    loadSchema(s"/test/$file.schema.json")

  private def loadSchema(path: String): Schema = {
    val stream = getClass.getResourceAsStream(path)
    val schemaText = Source.fromInputStream(stream).mkString
    stream.close()

    val schemaNode = objectMapper.readTree(schemaText)
    schemaRegistry.getSchema(schemaNode)
  }

  def apply[A](model: A, file: String)(implicit format: Format[A]): Unit = {
    val schema = retrieveSchema(file)

    val jsonStr = Json.prettyPrint(Json.toJson(model))
    val jsonNode: JsonNode = objectMapper.readTree(jsonStr)

    val errors = schema.validate(jsonNode)

    if (!errors.isEmpty) {
      errors.asScala.foreach { x =>
        logger.warn(
          s"failed to validate against json schema $file, " +
            s"schema: ${x.getSchemaLocation}, " +
            s"instance: ${x.getInstanceLocation}, " +
            s"problem: ${x.getKeyword}"
        )
      }
    }
  }

  def validate[A](model: A, file: String)(implicit format: Format[A]) = {
    val schema = retrieveSchema(file)

    val jsonStr = Json.prettyPrint(Json.toJson(model))
    val jsonNode: JsonNode = objectMapper.readTree(jsonStr)

    schema.validate(jsonNode).asScala.toList
  }
}

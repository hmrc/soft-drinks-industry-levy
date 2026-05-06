import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "10.7.0"
  private val hmrcMongoVersion = "2.12.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"            %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVersion"        % hmrcMongoVersion,
    "com.networknt"          % "json-schema-validator"            % "2.0.1" exclude ("com.fasterxml.jackson.core", "jackson-databind"),
    "org.typelevel"          %% "cats-core"                       % "2.13.0",
    "org.scala-stm"          %% "scala-stm"                       % "0.11.1",
    "io.github.openhtmltopdf" % "openhtmltopdf-pdfbox"            % "1.1.31"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"               %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc"               %% "stub-data-generator"           % "1.6.0",
    "uk.gov.hmrc.mongo"         %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.wiremock"               % "wiremock"                      % "3.13.2",
    "org.jsoup"                  % "jsoup"                         % "1.22.1",
    "org.scalatest"             %% "scalatest-funsuite"            % "3.2.19",
    "org.scalatestplus"         %% "mockito-5-18"                  % "3.2.19.0",
    "org.scalatestplus"         %% "scalacheck-1-17"               % "3.2.18.0",
    "org.apache.pekko"          %% "pekko-testkit"                 % "1.0.3"
  ).map(_ % "test, it")

  val all: Seq[ModuleID] = compile ++ test
}

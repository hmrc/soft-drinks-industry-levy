import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "10.4.0"
  private val hmrcMongoVersion = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"              %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc.mongo"        %% s"hmrc-mongo-$playVersion"        % hmrcMongoVersion,
    "com.github.fge"           %  "json-schema-validator"           % "2.2.6",
    "org.typelevel"            %% "cats-core"                       % "2.12.0",
    "org.scala-stm"            %% "scala-stm"                       % "0.11.1"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc"                %% "stub-data-generator"           % "1.5.0",
    "uk.gov.hmrc.mongo"          %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.wiremock"               % "wiremock"                       % "3.13.1",
    "com.fasterxml.jackson.core" %  "jackson-core"                  % "2.20.0",
    "com.fasterxml.jackson.core" %  "jackson-databind"              % "2.20.0",
    "org.jsoup"                  %  "jsoup"                         % "1.21.2",
    "org.scalatest"              %% "scalatest-funsuite"            % "3.2.19",
    "org.scalatestplus"          %% "mockito-5-18"                  % "3.2.19.0",
    "org.scalatestplus"          %% "scalacheck-1-17"               % "3.2.18.0",
    "org.apache.pekko"           %% "pekko-testkit"                 % "1.0.3"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}

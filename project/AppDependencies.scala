import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "8.6.0"
  private val hmrcMongoVersion = "2.2.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"              %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc.mongo"        %% s"hmrc-mongo-$playVersion"        % hmrcMongoVersion,
    "com.github.fge"           %  "json-schema-validator"           % "2.2.6",
    "org.typelevel"            %% "cats-core"                       % "2.12.0",
    "org.scala-stm"            %% "scala-stm"                       % "0.11.1",
    "com.github.ghik" % "silencer-lib" % "1.7.16" % Provided cross CrossVersion.full,
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.16" cross CrossVersion.full)
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc"                %% "stub-data-generator"           % "1.1.0",
    "uk.gov.hmrc.mongo"          %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.playframework"          %% "play-test"                     % play.core.PlayVersion.current,
    "com.fasterxml.jackson.core" %  "jackson-core"                  % "2.14.3",
    "com.fasterxml.jackson.core" %  "jackson-databind"              % "2.14.3",
    "org.wiremock"               %  "wiremock"                      % "3.4.2",
    "org.jsoup"                  %  "jsoup"                         % "1.18.1",
    "org.mockito"                %  "mockito-core"                  % "5.11.0",
    "org.scalacheck"             %% "scalacheck"                    % "1.17.0",
    "org.scalatest"              %% "scalatest-funsuite"            % "3.2.18",
    "org.scalatestplus"          %% "scalatestplus-scalacheck"      % "3.1.0.0-RC2",
    "org.scalatestplus"          %% "scalatestplus-mockito"         % "1.0.0-M2",
    "org.apache.pekko"           %% "pekko-testkit"                 % "1.0.3"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}

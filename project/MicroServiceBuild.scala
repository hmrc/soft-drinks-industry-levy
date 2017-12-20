import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion


object MicroServiceBuild extends Build with MicroService {

  val appName = "soft-drinks-industry-levy"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "6.13.0",
    "uk.gov.hmrc" %% "domain" % "5.0.0",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "6.0.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" % "mockito-core" % "2.7.22" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
    "org.scalacheck" %% "scalacheck" % "1.13.4" % scope,
    "uk.gov.hmrc" %% "stub-data-generator" % "0.4.0" % scope,
    "com.fasterxml.jackson.core" % "jackson-core" % "2.8.1" % scope,
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.1" % scope,
    "com.github.fge" % "json-schema-validator" % "2.2.6" % scope
  )

}

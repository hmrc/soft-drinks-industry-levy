import play.core.PlayVersion
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.PlayImport.{PlayKeys, ws}
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.SbtAutoBuildPlugin

object MicroService extends Build {

  lazy val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "1.7.0",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "6.1.0",
    "uk.gov.hmrc" %% "auth-client" % "2.6.0",
    "uk.gov.hmrc" %% "mongo-lock" % "5.1.0",
    "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided",
    "com.softwaremill.macwire" %% "macrosakka" % "2.3.1" % "provided",
    "com.softwaremill.macwire" %% "util" % "2.3.1",
    "com.softwaremill.macwire" %% "proxy" % "2.3.1",
    "org.typelevel" %% "cats-core" % "1.1.0",
    "com.github.fge" % "json-schema-validator" % "2.2.6",
    "com.github.pureconfig" %% "pureconfig" % "0.9.1"
  )

  lazy val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "com.typesafe.play" %% "play-test" % PlayVersion.current % "test",
    "org.mockito" % "mockito-core" % "2.19.1" % "test",
    "org.pegdown" % "pegdown" % "1.6.0" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
    "uk.gov.hmrc" %% "stub-data-generator" % "0.5.3" % "test",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.8.11" % "test",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.11.2" % "test",
    "com.github.tomakehurst" % "wiremock" % "2.18.0" % "test",
    "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % "test",
    "org.jsoup" % "jsoup" % "1.11.3" % "test"
  )

  lazy val microservice = Project("soft-drinks-industry-levy", file("."))
    .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin): _*)
    .settings(Seq(
      scalaVersion := "2.11.12",
      scalacOptions ++= Seq(
        "-Xlint",
        "-target:" + targetJvm.value,
        "-Xmax-classfile-name", "100",
        "-encoding", "UTF-8"
      ),
      javacOptions ++= Seq(
        "-Xlint",
        "-source", targetJvm.value.stripPrefix("jvm-"),
        "-target", targetJvm.value.stripPrefix("jvm-"),
        "-encoding", "UTF-8"
      )
    )
    )
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      libraryDependencies ++= compile ++ test,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
    )
    .settings(resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    ))
    .settings(PlayKeys.playDefaultPort := 8701)
    .settings(initialCommands in console := "import cats.implicits._")
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}

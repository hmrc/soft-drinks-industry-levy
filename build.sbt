import scoverage.ScoverageKeys
enablePlugins(
  play.sbt.PlayScala,
  SbtDistributablesPlugin
)

PlayKeys.playDefaultPort := 8701

libraryDependencies ++= AppDependencies.all

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.0",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.15.0"
)

// ================================================================================
// Dependencies
// ================================================================================
scalaVersion := "3.7.1"

// ================================================================================
// Compiler flags
// ================================================================================

scalacOptions ++= Seq(
//  "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Wconf:src=html/.*:s",
  "-Wconf:src=routes/.*:s",
  "-Wconf:msg=Flag.*repeatedly:s",
  "-Wconf:msg=unused implicit.*:s",
  "-Wconf:msg=unused explicit parameter:s"
)


// ================================================================================
// Misc
// ================================================================================

console / initialCommands := "import cats.implicits._"

majorVersion := 0

uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

// ================================================================================
// Testing
// ================================================================================
  import scoverage.ScoverageKeys._

  ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.models\.json.*;views\.html;.*\.Routes;.*\.RoutesPrefix;.*\.Reverse[^.]*;testonly"""
  coverageMinimumStmtTotal := 80
  coverageFailOnMinimum := true
  coverageHighlighting := true
  Compile / scalafmtOnCompile := true
  Test / scalafmtOnCompile := true
  ScoverageKeys.coverageExcludedFiles :=
    "<empty>;.*javascript;.*Routes.*;.*testonly.*;"+
      ".*BuildInfo.scala.*;.*controllers.test.*;.*connectors.TestConnector.*;.*models.*;"

disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
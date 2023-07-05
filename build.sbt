import scoverage.ScoverageKeys
enablePlugins(
  play.sbt.PlayScala,
  SbtDistributablesPlugin
)

PlayKeys.playDefaultPort := 8701

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core"  %  "jackson-core"        % "2.11.2",
  "com.fasterxml.jackson.core"  %  "jackson-databind"    % "2.11.2",
  "com.github.tomakehurst"      %  "wiremock-jre8"       % "2.27.1",
  "com.typesafe.play"           %% "play-test"           % play.core.PlayVersion.current,
  "org.jsoup"                   %  "jsoup"               % "1.15.4",
  "org.mockito"                 %  "mockito-core"        % "5.2.0",
  "org.pegdown"                 %  "pegdown"             % "1.6.0",
  "org.scalacheck"              %% "scalacheck"          % "1.15.4",
  "org.scalatest"               %% "scalatest"           % "3.2.9",
  "org.scalatest"               %% "scalatest-funsuite" % "3.2.9",
  "org.scalatestplus.play"      %% "scalatestplus-play"             % "5.1.0",
  "org.scalatestplus"           %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
  "org.scalatestplus"           %% "scalatestplus-mockito"   % "1.0.0-M2",
  "uk.gov.hmrc"                 %% "stub-data-generator" % "1.1.0",
  "com.typesafe.akka"           %% "akka-testkit"        % "2.6.20",
  "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-28"  % "0.73.0",
  "com.vladsch.flexmark"        % "flexmark-all"         % "0.36.8"
).map(_ % "test")

// ================================================================================
// Dependencies
// ================================================================================
scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  ws,
  "com.github.fge"            %  "json-schema-validator"         % "2.2.6",
  "com.github.pureconfig"     %% "pureconfig"                    % "0.17.4",
  "com.softwaremill.macwire"  %% "macros"                        % "2.5.8" % "provided",
  "com.softwaremill.macwire"  %% "macrosakka"                    % "2.5.8" % "provided",
  "com.softwaremill.macwire"  %% "proxy"                         % "2.5.8",
  "com.softwaremill.macwire"  %% "util"                          % "2.5.8",
  "org.typelevel"             %% "cats-core"                     % "2.9.0",
  "uk.gov.hmrc"               %% "bootstrap-backend-play-28"     % "5.21.0",
    "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-28"            % "0.73.0",
  "org.scala-stm"             %% "scala-stm"                     % "0.11.1",
  compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.17.13" cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib" % "1.17.13" % Provided cross CrossVersion.full
)

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
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused",                     // Warn if an import selector is not referenced.
  "-Ywarn-value-discard"                // Warn when non-Unit expression results are unused.
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
  import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
  import scoverage.ScoverageKeys._

  ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.models\.json.*;views\.html;.*\.Routes;.*\.RoutesPrefix;.*\.Reverse[^.]*;testonly"""
  coverageMinimumStmtTotal := 80
  coverageFailOnMinimum := false
  coverageHighlighting := true
  Compile / scalafmtOnCompile := true
  Test / scalafmtOnCompile := true
  ScoverageKeys.coverageExcludedFiles :=
    """<empty>;.*javascript;.*Routes.*;.*testonly.*;
      |.*BuildInfo.scala.*;.*controllers.test.*;.*connectors.TestConnector.*""".stripMargin

disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
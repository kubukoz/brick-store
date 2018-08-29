val macroParadise    = ("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full)
val kindProjector    = "org.spire-math" %% "kind-projector" % "0.9.7"
val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.2.4"

val plugins = List(macroParadise, kindProjector, betterMonadicFor)

val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

val http4s = Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.18.16",
  "org.http4s" %% "http4s-circe"        % "0.18.16",
  "org.http4s" %% "http4s-dsl"          % "0.18.16",
  "io.circe"   %% "circe-derivation"    % "0.9.0-M5" % Compile,
  "io.circe"   %% "circe-fs2"           % "0.9.0"
)

val cats =
  Seq(
    "org.typelevel"     %% "cats-core"      % "1.2.0",
    "io.chrisdavenport" %% "linebacker"     % "0.1.0",
    "io.chrisdavenport" %% "cats-par"       % "0.2.0",
    "org.typelevel"     %% "cats-effect"    % "0.10.1",
    "io.chrisdavenport" %% "log4cats-slf4j" % "0.1.0"
  )

val doobie = Seq(
  "org.tpolecat"   %% "doobie-core"      % "0.5.3",
  "org.tpolecat"   %% "doobie-postgres"  % "0.5.3",
  "org.tpolecat"   %% "doobie-hikari"    % "0.5.3",
  "org.tpolecat"   %% "doobie-scalatest" % "0.5.3",
  "org.postgresql" % "postgresql"        % "42.2.4",
  "org.flywaydb"   % "flyway-core"       % "5.1.4"
)

val chimney = "io.scalaland" %% "chimney" % "0.2.1"

val tsec =
  Seq(
    "io.github.jmcardon" %% "tsec-jwt-mac"  % "0.0.1-M11",
    "io.github.jmcardon" %% "tsec-password" % "0.0.1-M11"
  )

val pureconfig = Seq(
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.9.2",
  "com.github.pureconfig" %% "pureconfig-enumeratum"  % "0.9.2"
)

val enumeratum = Seq(
  "com.beachape" %% "enumeratum-circe" % "1.5.13"
)

val macwire = Seq(
  "com.softwaremill.macwire" %% "macros" % "2.3.1" % Provided,
  "com.softwaremill.macwire" %% "util"   % "2.3.1",
  "com.softwaremill.macwire" %% "proxy"  % "2.3.1"
)

lazy val root = (project in file(".")).settings(
  organization := "org.typelevel",
  name := "brick-store",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.6",
  scalacOptions ++= Options.flags,
  libraryDependencies ++= plugins
    .map(compilerPlugin) ++ http4s ++ doobie ++ cats ++ tsec ++ macwire ++ enumeratum ++ pureconfig ++ Seq(logback,
                                                                                                           chimney)
)

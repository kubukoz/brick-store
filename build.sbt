val macroParadise    = ("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full)
val kindProjector    = "org.typelevel" %% "kind-projector" % "0.10.3"
val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

val plugins = List(macroParadise, kindProjector, betterMonadicFor)

val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

val http4s = Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.20.11",
  "org.http4s" %% "http4s-circe"        % "0.20.11",
  "org.http4s" %% "http4s-dsl"          % "0.20.11",
  "io.circe"   %% "circe-derivation"    % "0.12.0-M5" % Compile,
  "io.circe"   %% "circe-fs2"           % "0.11.0"
)

val cats =
  Seq(
    "org.typelevel"     %% "cats-core"      % "1.6.1",
    "io.chrisdavenport" %% "cats-par"       % "0.2.1",
    "org.typelevel"     %% "cats-effect"    % "1.4.0",
    "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0"
  )

val doobie = Seq(
  "org.tpolecat"   %% "doobie-core"      % "0.8.4",
  "org.tpolecat"   %% "doobie-postgres"  % "0.8.4",
  "org.tpolecat"   %% "doobie-hikari"    % "0.8.4",
  "org.tpolecat"   %% "doobie-scalatest" % "0.8.4",
  "org.postgresql" % "postgresql"        % "42.2.8",
  "org.flywaydb"   % "flyway-core"       % "6.0.6"
)

val chimney = "io.scalaland" %% "chimney" % "0.3.3"

val pureconfig = Seq(
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.11.1",
  "com.github.pureconfig" %% "pureconfig-enumeratum"  % "0.11.1",
  "com.github.pureconfig" %% "pureconfig-generic"     % "0.11.1"
)

val enumeratum = Seq(
  "com.beachape" %% "enumeratum-circe" % "1.5.21"
)

val macwire = Seq(
  "com.softwaremill.macwire" %% "macros" % "2.3.3" % Provided,
  "com.softwaremill.macwire" %% "util"   % "2.3.3",
  "com.softwaremill.macwire" %% "proxy"  % "2.3.3"
)

lazy val root = (project in file(".")).settings(
  organization := "org.typelevel",
  name := "brick-store",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.7",
  scalacOptions ++= Options.flags,
  libraryDependencies ++= plugins
    .map(compilerPlugin) ++ http4s ++ doobie ++ cats ++ macwire ++ enumeratum ++ pureconfig ++ Seq(logback, chimney)
)

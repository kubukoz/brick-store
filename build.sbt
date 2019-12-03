val kindProjector    = ("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full)
val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

val plugins = List(kindProjector, betterMonadicFor)

val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

val http4s = Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.21.0-M5",
  "org.http4s" %% "http4s-circe"        % "0.21.0-M5",
  "org.http4s" %% "http4s-dsl"          % "0.21.0-M5",
  "io.circe"   %% "circe-derivation"    % "0.12.0-M7" % Compile,
  "io.circe"   %% "circe-fs2"           % "0.12.0"
)

val cats =
  Seq(
    "org.typelevel"     %% "cats-core"           % "2.0.0",
    "org.typelevel"     %% "cats-tagless-macros" % "0.10",
    "org.typelevel"     %% "cats-effect"         % "2.0.0",
    "io.chrisdavenport" %% "log4cats-slf4j"      % "1.0.1"
  )

val doobie = Seq(
  "org.tpolecat"   %% "doobie-core"      % "0.8.6",
  "org.tpolecat"   %% "doobie-postgres"  % "0.8.6",
  "org.tpolecat"   %% "doobie-hikari"    % "0.8.6",
  "org.tpolecat"   %% "doobie-scalatest" % "0.8.6",
  "org.postgresql" % "postgresql"        % "42.2.8",
  "org.flywaydb"   % "flyway-core"       % "6.1.0"
)

val chimney = "io.scalaland" %% "chimney" % "0.3.5"

val pureconfig = Seq(
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.12.1",
  "com.github.pureconfig" %% "pureconfig-enumeratum"  % "0.12.1",
  "com.github.pureconfig" %% "pureconfig-generic"     % "0.12.1"
)

val enumeratum = Seq(
  "com.beachape" %% "enumeratum-circe" % "1.5.22"
)

lazy val root = (project in file(".")).settings(
  organization := "org.typelevel",
  name := "brick-store",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.13.1",
  libraryDependencies ++= plugins
    .map(compilerPlugin) ++ http4s ++ doobie ++ cats ++ enumeratum ++ pureconfig ++ Seq(logback, chimney)
)

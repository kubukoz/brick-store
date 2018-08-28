package org.typelevel.brickstore

import cats.effect.{Effect, IO}
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import fs2.{Stream, StreamApp}
import Stream.{eval => SE}
import org.flywaydb.core.Flyway
import org.http4s.server.blaze.BlazeBuilder
import org.typelevel.brickstore.config.DbConfig
import org.typelevel.brickstore.module.{MainModule, Module}

class Application[F[_]](implicit F: Effect[F]) extends StreamApp[F] {
  private val configF: F[DbConfig] = pureconfig.module.catseffect.loadConfigF[F, DbConfig]("db")

  private def buildModule(transactor: Transactor[F]): Module[F] = new MainModule(transactor)

  private def transactorStream(config: DbConfig): Stream[F, HikariTransactor[F]] =
    HikariTransactor.stream("org.postgresql.Driver", config.jdbcUrl, config.user, config.password)

  import scala.concurrent.ExecutionContext.Implicits.global

  private def runMigrations(config: DbConfig): F[Unit] =
    F.delay {
      val flyway = new Flyway()
      flyway.setDataSource(config.jdbcUrl, config.user, config.password)
      flyway.migrate()
    }.void

  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, StreamApp.ExitCode] =
    for {
      config     <- SE(configF)
      transactor <- transactorStream(config)
      _          <- SE(runMigrations(config))

      module = buildModule(transactor)
      server = BlazeBuilder[F].bindHttp().mountService(module.bricksController.service, "/")

      exitCode <- server.serve
    } yield exitCode
}

object Main extends Application[IO]

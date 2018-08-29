package org.typelevel.brickstore

import cats.effect.{Effect, IO}
import cats.implicits._
import cats.temp.par.Par
import doobie.hikari.HikariTransactor
import fs2.Stream.{eval => SE}
import fs2.{Stream, StreamApp}
import org.flywaydb.core.Flyway
import org.http4s
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeBuilder
import org.typelevel.brickstore.config.DbConfig
import org.typelevel.brickstore.module.{MainModule, Module}

class Application[F[_]: Par](implicit F: Effect[F]) extends StreamApp[F] {
  private val configF: F[DbConfig] = pureconfig.module.catseffect.loadConfigF[F, DbConfig]("db")

  private def transactorStream(config: DbConfig): Stream[F, HikariTransactor[F]] =
    HikariTransactor.stream("org.postgresql.Driver", config.jdbcUrl, config.user, config.password)

  import scala.concurrent.ExecutionContext.Implicits.global

  private def runMigrations(config: DbConfig): F[Unit] =
    F.delay {
      val flyway = new Flyway()
      flyway.setDataSource(config.jdbcUrl, config.user, config.password)
      flyway.migrate()
    }.void

  private def mergeServices(module: Module[F]): http4s.HttpService[F] = {
    Router(
      "/bricks" -> module.bricksController.service,
      "/cart"   -> module.cartController.service,
      "/order"  -> module.orderController.service
    )
  }

  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, StreamApp.ExitCode] =
    for {
      config     <- SE(configF)
      transactor <- transactorStream(config)
      _          <- SE(runMigrations(config))

      module <- SE(MainModule.make(transactor))
      server = BlazeBuilder[F].bindHttp().mountService(mergeServices(module))

      exitCode <- server.serve
    } yield exitCode
}

object Main extends Application[IO]

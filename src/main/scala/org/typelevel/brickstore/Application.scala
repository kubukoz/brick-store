package org.typelevel.brickstore

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, IO}
import cats.implicits._
import cats.temp.par.Par
import cats.~>
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Interpreter
import fs2.Stream.{eval => SE}
import fs2.{Stream, StreamApp}
import io.chrisdavenport.linebacker.DualContext
import io.chrisdavenport.linebacker.contexts.Executors
import org.flywaydb.core.Flyway
import org.http4s
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeBuilder
import org.typelevel.brickstore.config.DbConfig
import org.typelevel.brickstore.module.{MainModule, Module}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

class Application[F[_]: Par](implicit F: ConcurrentEffect[F]) extends StreamApp[F] {
  private val configF: F[DbConfig] = pureconfig.module.catseffect.loadConfigF[F, DbConfig]("db")

  private def transactorStream(config: DbConfig): Stream[F, HikariTransactor[F]] = {
    HikariTransactor.stream("org.postgresql.Driver", config.jdbcUrl, config.user, config.password)
  }

  private def injectDualContext(dualContext: DualContext[F])(transactor: HikariTransactor[F]): HikariTransactor[F] = {
    def injectTransformation(interpreter: Interpreter[F], trans: F ~> F): Interpreter[F] = {
      type G[A] = Kleisli[F, Connection, A]
      interpreter.andThen(λ[G ~> G](_.mapK(trans)))
    }

    transactor.copy(interpret0 = injectTransformation(transactor.interpret, λ[F ~> F](dualContext.block(_))))
  }

  private val buildDualContext: Stream[F, DualContext[F]] = Executors.fixedPool(10).map { blockingExecutor =>
    DualContext.fromContexts(
      implicitly[ExecutionContext],
      ExecutionContext.fromExecutorService(blockingExecutor)
    )
  }

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
      config <- SE(configF)
      _      <- SE(runMigrations(config))

      ec <- Executors.fixedPool[F](10).map(ExecutionContext.fromExecutorService)

      dualContext <- buildDualContext
      transactor  <- transactorStream(config).map(injectDualContext(dualContext))

      module <- locally {
        implicit val ecc: ExecutionContextExecutorService = ec
        SE(MainModule.make(transactor))
      }

      server = BlazeBuilder[F].bindHttp().mountService(mergeServices(module))

      exitCode <- server.serve
    } yield exitCode
}

object Main extends Application[IO]

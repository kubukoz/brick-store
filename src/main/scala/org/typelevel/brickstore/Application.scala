package org.typelevel.brickstore

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, IO}
import cats.implicits._
import cats.temp.par.Par
import cats.~>
import doobie.hikari.HikariTransactor
import doobie.util.transactor.{Interpreter, Transactor}
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

  /**
    * loading config from application.conf.
    * it's an effect (reading from classpath resources + decoding to a case class), so F[_]
    */
  private val configF: F[DbConfig] = pureconfig.module.catseffect.loadConfigF[F, DbConfig]("db")

  /**
    * Building a Doobie transactor - a pure wrapper over a JDBC connection [pool] (in this case, HikariCP)
    * */
  private def transactorStream(config: DbConfig): Stream[F, HikariTransactor[F]] = {
    HikariTransactor.stream("org.postgresql.Driver", config.jdbcUrl, config.user, config.password)
  }

  /**
    * Injects the `dualContext.block` wrapper to the transactor
    * to ensure DB calls block threads only in the thread pool designated for it
    *
    * @tparam A0 the transactor's underlying type (in the case of HikariTransactor it's a HikariDataSource)
    * */
  private def injectDualContext[A0](dualContext: DualContext[F])(
    transactor: Transactor.Aux[F, A0]): Transactor.Aux[F, A0] = {
    def injectTransformation(interpreter: Interpreter[F], trans: F ~> F): Interpreter[F] = {
      type G[A] = Kleisli[F, Connection, A]
      interpreter.andThen(λ[G ~> G](_.mapK(trans)))
    }

    transactor.copy(interpret0 = injectTransformation(transactor.interpret, λ[F ~> F](dualContext.block(_))))
  }

  /**
    * Allows executing an F[A] on the blocking thread pool.
    * called DualContext because it shifts execution to the blocking pool,
    * and comes back to the default pool afterwards (in this case, implicit - global EC).
    * */
  private val buildDualContext: Stream[F, DualContext[F]] = Executors.fixedPool(10).map { blockingExecutor =>
    DualContext.fromContexts(
      implicitly[ExecutionContext],
      ExecutionContext.fromExecutorService(blockingExecutor)
    )
  }

  /**
    * Runs DB migrations with Flyway.
    * */
  private def runMigrations(config: DbConfig): F[Unit] =
    F.delay {
      val flyway = new Flyway()
      flyway.setDataSource(config.jdbcUrl, config.user, config.password)
      flyway.migrate()
    }.void

  /**
    * Merges all the http4s services in the module into one, each with its prefix.
    * */
  private def mergeServices(module: Module[F]): http4s.HttpService[F] = {
    Router(
      "/bricks" -> module.bricksController.service,
      "/cart"   -> module.cartController.service,
      "/order"  -> module.orderController.service
    )
  }

  /**
    * StreamApp's `main` equivalent. If you execute `requestShutdown`, the application will gracefully stop.
    * */
  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, StreamApp.ExitCode] =
    for {
      //SE is Stream.eval (see imports)
      config <- SE(configF)
      _      <- SE(runMigrations(config))

      dualContext <- buildDualContext
      transactor  <- transactorStream(config).map(injectDualContext(dualContext))

      module <- SE(MainModule.make(transactor))

      server = BlazeBuilder[F].bindHttp().mountService(mergeServices(module))

      exitCode <- server.serve
    } yield exitCode
}

object Main extends Application[IO]

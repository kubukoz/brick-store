package org.typelevel.brickstore.app

import cats.effect._
import cats.implicits._
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.logging.LogFactory
import org.flywaydb.core.internal.logging.slf4j.Slf4jLogCreator
import org.http4s
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.brickstore.app.config.DbConfig
import org.typelevel.brickstore.app.module.{MainModule, Module}

import scala.concurrent.duration.Duration
import cats.Parallel
import pureconfig.ConfigSource
import doobie.util.transactor.Transactor

class Application[F[_]: Parallel: ContextShift: Timer](implicit F: ConcurrentEffect[F]) {

  /**
    * loading config from application.conf.
    * it's an effect (reading from classpath resources + decoding to a case class), so F[_]
    */
  private val configF: F[DbConfig] = {
    import pureconfig.generic.auto._

    F.delay {
      ConfigSource.fromConfig(ConfigFactory.load(this.getClass.getClassLoader)).at("db").loadOrThrow[DbConfig]
    }
  }

  /**
    * Building a Doobie transactor - a pure wrapper over a JDBC connection [pool] (in this case, HikariCP)
    * */
  private def transactorResource(config: DbConfig, blocker: Blocker): Resource[F, HikariTransactor[F]] = {
    for {
      connectEC <- ExecutionContexts.fixedThreadPool(10)
      transactor <- HikariTransactor.newHikariTransactor(
        "org.postgresql.Driver",
        config.jdbcUrl,
        config.user,
        config.password,
        connectEC,
        blocker
      )
    } yield transactor
  }

  /**
    * Runs DB migrations with Flyway.
    * */
  private def runMigrations(config: DbConfig): F[Unit] = {
    F.delay {
      Flyway
        .configure(getClass.getClassLoader)
        .dataSource(config.jdbcUrl, config.user, config.password)
        .load()
        .migrate()
    }.void
  }

  /**
    * Merges all the http4s routes in the module into one, each with its prefix.
    * */
  private def routes(module: Module[F]): http4s.HttpRoutes[F] = {
    Router(
      "/bricks" -> module.bricksController.routes,
      "/cart"   -> module.cartController.routes,
      "/order"  -> module.orderController.routes
    )
  }

  /**
    * IOApp's `main` equivalent.
    * */
  val run: F[Nothing] = {
    for {
      config                               <- Resource.liftF(configF).evalTap(runMigrations)
      blocker                              <- Blocker[F]
      implicit0(transactor: Transactor[F]) <- transactorResource(config, blocker)

      module <- Resource.liftF(MainModule.make[F])
      //infinite duration so that we don't timeout errors when requesting a streaming endpoint like /order/stream
      _ <- BlazeServerBuilder[F]
        .bindHttp()
        .withIdleTimeout(Duration.Inf)
        .withHttpApp(routes(module).orNotFound)
        .resource
    } yield ()
  }.use[Nothing](_ => F.never)
}

object Main extends IOApp {
  //this line saves the world (workaround for Flyway doing weird stuff with static fields)
  LogFactory.setLogCreator(new Slf4jLogCreator)

  def run(args: List[String]): IO[ExitCode] = new Application[IO].run
}

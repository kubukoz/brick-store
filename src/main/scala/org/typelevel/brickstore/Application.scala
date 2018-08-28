package org.typelevel.brickstore

import cats.effect.{Effect, IO}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze.BlazeBuilder
import org.typelevel.brickstore.module.{MainModule, Module}

class Application[F[_]: Effect] extends StreamApp[F] {

  def buildModule(transactor: Transactor[F]): Module[F] = new MainModule(transactor)

  private val transactorStream: Stream[F, HikariTransactor[F]] =
    HikariTransactor.stream("org.postgresql.Driver", "jdbc:postgresql:postgres", "postgres", "")

  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, StreamApp.ExitCode] =
    for {
      transactor <- transactorStream

      module = buildModule(transactor)
      server = BlazeBuilder[F].bindHttp().mountService(module.bricksController.service, "/")

      exitCode <- server.serve
    } yield exitCode
}

object Main extends Application[IO]

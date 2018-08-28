package org.typelevel.brickstore

import cats.effect.{Effect, IO}
import fs2.{Stream, StreamApp}
import org.http4s.HttpService
import org.http4s.server.blaze.BlazeBuilder

import scala.language.higherKinds

class Application[F[_]: Effect] extends StreamApp[F] {
  val helloWorldService: HttpService[F] = new HelloWorldService[F].service

  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, StreamApp.ExitCode] =
    BlazeBuilder[F].bindHttp(8080, "0.0.0.0").mountService(helloWorldService, "/").serve
}

object Main extends Application[IO]

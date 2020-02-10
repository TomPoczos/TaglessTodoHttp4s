package todo

import cats.effect.{ExitCode, IO, IOApp}
import doobie.Transactor
import Interpreters.Doobie

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val config = Config

    val transactor = Transactor.fromDriverManager[IO](
      Config.dbDriver,
      Config.dbUrl
    )
    val migrations =
      Migrations(
        transactor
      )
    val server =
      Server(
        Routes(
          Doobie(
            transactor
          )
        )
      )

    for {
      _ <- migrations.run
      serverResource = server.run
      _ <- serverResource.use(_ => IO.never)
    } yield ExitCode.Success
  }
}

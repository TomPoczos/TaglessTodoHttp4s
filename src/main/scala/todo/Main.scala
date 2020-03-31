package todo

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import doobie.Transactor
import todo.Interpreters.Doobie

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val config = Config

    implicit val transactor = Transactor.fromDriverManager[IO](
      Config.dbDriver,
      Config.dbUrl
    )
    implicit val migrations = Migrations[IO]
    implicit val doobie = Doobie[IO]
    implicit val auth = Authentication[IO]
    implicit val routes = Routes[IO]
    implicit val server = Server[IO]

    migrations.run *> server.resource.use(_ => IO.never) *> IO(ExitCode.Success)
  }
}

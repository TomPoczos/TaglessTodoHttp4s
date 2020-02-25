package todo

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import doobie.Transactor
import todo.Interpreters.Doobie

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
    val doobie = Doobie(
      transactor
    )
    val serverResource =
      Server(
        Routes(
          Authentication(
            doobie
          ),
          doobie
        )
      ).resource

    migrations.run *> serverResource.use(_ => IO.never) *> IO(ExitCode.Success)
  }
}

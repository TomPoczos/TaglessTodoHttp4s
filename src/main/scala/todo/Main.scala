package todo

import cats.effect.{ExitCode, IO, IOApp}
import doobie.Transactor
import todo.dataaccess.Interpreters.Doobie

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val transactor = Transactor.fromDriverManager[IO](
      "org.sqlite.JDBC",
      "jdbc:sqlite:todo.db"
    )
    val migrations = Migrations(
      transactor
    )
    val server = Server(
      Routes(
        Doobie(
          transactor
        )
      )
    )

    for {
      _ <- migrations.run
      _ <- server.run
    } yield ExitCode.Success
  }
}

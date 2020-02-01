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

    for {
      _ <- Migrations.run(transactor)
      _ <- Server.run(new Doobie[IO](transactor))
    } yield ExitCode.Success
  }
}

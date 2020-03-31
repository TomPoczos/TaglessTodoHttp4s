package todo

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import doobie.Transactor
import org.reactormonk.{CryptoBits, PrivateKey}
import dataaccess.interpreters._

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    implicit val config = Config

    implicit val crypto =
      CryptoBits(
        PrivateKey(
          scala.io.Codec.toUTF8(config.encryptionKey)
        )
      )

    implicit val transactor = Transactor.fromDriverManager[IO](
      Config.dbDriver,
      Config.dbUrl
    )
    implicit val migrations = MigrationsInterpreter[IO]
    implicit val userDao = UserDaoInterpreter[IO]
    implicit val todoDao = TodoDaoInterpreter[IO]
    implicit val userService = UserService[IO]
    implicit val routes = Routes[IO].router
    implicit val server = Server[IO]

    migrations.run *> server.resource.use(_ => IO.never) *> IO(ExitCode.Success)
  }
}

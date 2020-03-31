package todo.dataaccess.interpreters

import Model._
import cats.implicits._
import cats.data.OptionT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import todo.dataaccess.Algebras._

class UserDaoInterpreter[F[_]: Sync: Transactor] extends UserDao[F] {
  override def findUser(userId: UserId): F[Option[User]] = {
    sql"select id, name, salt, pwdHash from user where id = ${userId.value}"
      .query[User]
      .option
      .transact(F)
  }

  override def findUserByName(username: Username): OptionT[F, User] =
    OptionT(
      sql"select id, name, salt, pwdHash from user where name = ${username.value}"
        .query[User]
        .option
        .transact(F)
    )

  override def createUser(name: Username, salt: Salt, pwdHash: PwdHash): F[Int] = {
    sql"""
          | insert into user (name, salt, pwdHash)
          | values (${name.value}, ${salt.value}, ${pwdHash.value})
          |""".stripMargin.update.run.transact(F)
  }
}

object UserDaoInterpreter {
  def apply[F[_]: Sync: Transactor] = new UserDaoInterpreter[F]
}

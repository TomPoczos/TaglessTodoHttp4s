package todo

import Model._
import cats.data.OptionT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import todo.Algebras.UserDao

object Algebras {

  trait TodoDao[F[_]] {

    def findTodos(userId:  UserId): F[List[Todo]]
    def createTodo(name:   String, userId: UserId): F[Int]
    def markAsDone(todoId: TodoId, userId: UserId): F[Int]
  }

  trait UserDao[F[_]] {
    def findUser(userId:         UserId): F[Option[User]]
    def findUserByName(username: Username): OptionT[F, User]
    def createUser(name:         Username, salt: Salt, pwdHash: PwdHash): F[Int]
  }
}

object Interpreters {

  import Algebras.TodoDao

  class Doobie[F[_]: Sync: Transactor] extends TodoDao[F] with UserDao[F] {
    override def findTodos(userId: UserId): F[List[Todo]] =
      sql"select id, name, done from todo where user_fk = ${userId.value}"
        .query[Todo]
        .to[List]
        .transact(F)

    override def createTodo(name: String, userId: UserId): F[Int] =
      sql"insert into todo (user_fk, name, done) values (${userId.value}, ${name}, 0)".update.run.transact(F)

    override def markAsDone(todoId: TodoId, userId: UserId): F[Int] =
      sql"update todo set done = 1 where user_fk = ${userId.value} and id = ${todoId.value}".update.run
        .transact(F)

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

  object Doobie {
    def apply[F[_]: Sync: Transactor] = new Doobie[F]
  }
}

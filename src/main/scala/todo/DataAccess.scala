package todo

import Model.{Todo, User}
import cats.data.OptionT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import todo.Algebras.UserDao

object Algebras {

  trait TodoDao[F[_]] {

    def findTodos(userId: User.Id): F[List[Todo]]

    def createTodo(name: String, userId: User.Id): F[Int]

    def markAsDone(id: Int): F[Int]
  }

  trait UserDao[F[_]] {
    def findUser(userId:         User.Id):   F[Option[User]]
    def findUserByName(username: User.Name): OptionT[F, User]
    def createUser(User:         User):      F[Int]
  }
}

object Interpreters {

  import Algebras.TodoDao

  class Doobie[F[_]: Sync](transactor: Transactor[F]) extends TodoDao[F] with UserDao[F] {
    override def findTodos(userId: User.Id): F[List[Todo]] =
      sql"select id, name, done from todo where user_fk = ${userId.value}"
        .query[Todo]
        .to[List]
        .transact(transactor)

    override def createTodo(name: String, userId: User.Id): F[Int] =
      sql"insert into todo (user_fk, name, done) values (${userId.value}, ${name}, 0)"
        .update.run.transact(transactor)

    override def markAsDone(id: Int): F[Int] =
      sql"update todo set done = 1 where id = $id".update.run
        .transact(transactor)

    override def findUser(userId: User.Id): F[Option[User]] = {
      sql"select id, name, salt, pwdHash from user where id = ${userId.value}"
        .query[User]
        .option
        .transact(transactor)
    }

    override def findUserByName(username: User.Name): OptionT[F, User] =
      OptionT(
        sql"select id, name, salt, pwdHash from user where name = ${username.value}"
          .query[User]
          .option
          .transact(transactor)
      )

    override def createUser(user: User): F[Int] = {
      sql"""
          | insert into user (name, salt, pwdHash)
          | values (${user.name.value}, ${user.salt.value}, ${user.pwdHash.value})
          |""".stripMargin.update.run.transact(transactor)
    }
  }

  object Doobie {
    def apply[F[_]: Sync](xa: Transactor[F]) = new Doobie[F](xa)
  }
}

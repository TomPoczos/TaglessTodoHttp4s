package todo

import Model.{Todo, User}
import cats.data.OptionT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import todo.Algebras.UserDao

object Algebras {

  trait TodoDao[F[_]] {

    def findTodos(userId:  User.Id): F[List[Todo]]
    def createTodo(name:   String, userId: User.Id): F[Int]
    def markAsDone(todoId: Todo.Id, userId: User.Id): F[Int]
  }

  trait UserDao[F[_]] {
    def findUser(userId:         User.Id): F[Option[User]]
    def findUserByName(username: User.Name): OptionT[F, User]
    def createUser(name:         User.Name, salt: User.Salt, pwdHash: User.PwdHash): F[Int]
  }
}

object Interpreters {

  import Algebras.TodoDao

  class Doobie[F[_]: Sync: Transactor] extends TodoDao[F] with UserDao[F] {
    override def findTodos(userId: User.Id): F[List[Todo]] =
      sql"select id, name, done from todo where user_fk = ${userId.value}"
        .query[Todo]
        .to[List]
        .transact(F)

    override def createTodo(name: String, userId: User.Id): F[Int] =
      sql"insert into todo (user_fk, name, done) values (${userId.value}, ${name}, 0)".update.run.transact(F)

    override def markAsDone(todoId: Todo.Id, userId: User.Id): F[Int] =
      sql"update todo set done = 1 where user_fk = ${userId.value} and id = ${todoId.value}".update.run
        .transact(F)

    override def findUser(userId: User.Id): F[Option[User]] = {
      sql"select id, name, salt, pwdHash from user where id = ${userId.value}"
        .query[User]
        .option
        .transact(F)
    }

    override def findUserByName(username: User.Name): OptionT[F, User] =
      OptionT(
        sql"select id, name, salt, pwdHash from user where name = ${username.value}"
          .query[User]
          .option
          .transact(F)
      )

    override def createUser(name: User.Name, salt: User.Salt, pwdHash: User.PwdHash): F[Int] = {
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

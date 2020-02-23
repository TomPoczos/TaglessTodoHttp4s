package todo

import Model.{Todo, User}
import cats.data.OptionT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import todo.Algebras.UserDao

object Algebras {

  trait TodoDao[F[_]] {

    def findAll(): F[List[Todo]]

    def create(name: String): F[Int]

    def markAsDone(id: Int): F[Int]
  }

  trait UserDao[F[_]] {
    def find(userId:         User.Id):   F[Option[User]]
    def findByName(username: User.Name): OptionT[F, User]
    def create(User:         User):      F[Int]
  }
}

object Interpreters {

  import Algebras.TodoDao

  class Doobie[F[_]: Sync](transactor: Transactor[F]) extends TodoDao[F] with UserDao[F] {
    override def findAll(): F[List[Todo]] =
      sql"select id, name, done from todo"
        .query[Todo]
        .to[List]
        .transact(transactor)

    override def create(name: String): F[Int] =
      sql"insert into todo (name, done) values ($name, 0)".update.run
        .transact(transactor)

    override def markAsDone(id: Int): F[Int] =
      sql"update todo set done = 1 where id = $id".update.run
        .transact(transactor)

    override def find(userId: User.Id): F[Option[User]] =
      sql"select id, name, salt, pwdHash from user where id = '${userId.value}'"
        .query[User]
        .option
        .transact(transactor)

    override def findByName(username: User.Name): OptionT[F, User] =
      OptionT(
        sql"select id, name, salt, pwdHash from user where id = '${username.value}'"
          .query[User]
          .option
          .transact(transactor)
      )

    override def create(user: User): F[Int] = {
      // apparently I get autoincrement for free
      // https://www.sqlite.org/faq.html#q1

      sql"""
          | insert into user (name, salt, pwdHash)
          | values ${user.name.value}, ${user.salt.value}, ${user.pwdHash.value}
          |""".stripMargin.update.run.transact(transactor)
    }
  }

  object Doobie {
    def apply[F[_]: Sync](xa: Transactor[F]) = new Doobie[F](xa)
  }
}

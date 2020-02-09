package todo.dataaccess

import Model.Todo
import cats.effect.{Async, IO, Sync}
import doobie.Transactor

import doobie.implicits._

object Algebras {

  trait TodoDao[F[_], G[_]] {

    def findAll(): F[G[Todo]]

    def create(name: String): F[Int]

    def markAsDone(id: Int): F[Int]
  }

  object TodoDao {
    def apply[F[_], G[_]](implicit ev: TodoDao[F, G]): TodoDao[F, G] = ev
  }
}

object Interpreters {

  import todo.dataaccess.Algebras.TodoDao

  class Doobie[F[_]:Sync](transactor: Transactor[F]) extends TodoDao[F, List] {
    override def findAll(): F[List[Todo]] =
      sql"select id, name, done from todo"
        .query[Todo]
        .to[List]
        .transact(transactor)

    override def create(name: String): F[Int] =
      sql"insert into todo (name, done) values ($name, 0)"
        .update
        .run
        .transact(transactor)

    override def markAsDone(id: Int): F[Int] =
      sql"update todo set done = 1 where id = $id"
        .update
        .run
        .transact(transactor)
  }

  object Doobie {
    def apply[F[_]:Sync](xa: Transactor[F]) = new Doobie[F](xa)
  }
}


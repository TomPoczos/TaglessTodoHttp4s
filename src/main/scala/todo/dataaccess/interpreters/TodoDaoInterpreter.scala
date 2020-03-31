package todo.dataaccess.interpreters

import Model._
import cats.implicits._
import cats.data.OptionT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import todo.dataaccess.Algebras._

class TodoDaoInterpreter[F[_]: Sync: Transactor] extends TodoDao[F] {
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
}

object TodoDaoInterpreter {
  def apply[F[_]: Sync: Transactor] = new TodoDaoInterpreter[F]
}

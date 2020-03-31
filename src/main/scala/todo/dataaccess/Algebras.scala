package todo.dataaccess

import Model._
import cats.data.OptionT

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

  trait Migrations[F[_]] {
    def run: F[List[Int]] 
  }
}

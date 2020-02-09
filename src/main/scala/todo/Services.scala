package todo

import Model.{Authenticated, Login, User}
import cats.{Functor, Monad}
import cats.implicits._
import todo.Algebras.UserDao

package object Services {
  class UserService[F[-_]:Monad](dao: UserDao[F]) {
    def authenticate(login: Login): F[Authenticated] = {
      dao
        .find(User.Name(login.username.value))
        .filter(user => false)
        .getOrElse(Authenticated(false))

    }
  }

}

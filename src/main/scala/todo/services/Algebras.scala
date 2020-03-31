package todo.services

import todo.Model._

package object Algebras {
  trait UserService[F[_]] {

    def createuser(login:   Login): F[Int]
    def authenticate(login: Login): F[Either[ErrorMsg, Token]]
  }
}

package todo

import Model._
import cats.effect._
import cats.implicits._
import com.github.t3hnar.bcrypt._
import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
import org.http4s.dsl.Http4sDsl
import org.reactormonk.CryptoBits
import todo.Algebras.UserDao

import scala.concurrent.duration.MILLISECONDS

class UserService[F[_]:Sync:UserDao](implicit crypto: CryptoBits)
    extends Http4sDsl[F]
    with CirceInstances
    with CirceEntityEncoder {

  def createuser(login: Login) = {
    val salt = generateSalt
    F.createUser(
        Username(login.username.value),
        Salt(salt),
        PwdHash((salt + login.password.value).bcrypt)
    )
  }

  def authenticate(login: Login): F[Either[ErrorMsg, Token]] = {
    def validatePassword(hash: PwdHash, salt: Salt): Boolean =
      (salt.value + login.password.value).isBcrypted(hash.value)

    F.findUserByName(login.username)
      .filter(user => validatePassword(user.pwdHash, user.salt))
      .semiflatMap { user =>
        clock
          .monotonic(MILLISECONDS)
          .map(time => Token(crypto.signToken(user.id.value.toString, time.toString)))
      }
      .toRight(ErrorMsg("Invalid Credentials"))
      .value
  }

  private val clock = Clock.create


}

object UserService {
  def apply[F[_]:Sync:UserDao](implicit crypto: CryptoBits) = new UserService()
}

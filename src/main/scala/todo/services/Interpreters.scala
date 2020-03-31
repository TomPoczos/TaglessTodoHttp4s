package todo

package object Interpreters {
  import todo.Model._
  import cats.effect._
  import cats.implicits._
  import com.github.t3hnar.bcrypt._
  import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
  import org.http4s.dsl.Http4sDsl
  import org.reactormonk.CryptoBits
  import todo.dataaccess.Algebras.UserDao
  import todo.services.Algebras.UserService

  import scala.concurrent.duration.MILLISECONDS

  class UserServiceInterpreter[F[_]: Sync: UserDao](implicit crypto: CryptoBits)
      extends UserService[F]
      with Http4sDsl[F]
      with CirceInstances
      with CirceEntityEncoder {

    override def createuser(login: Login) = {
      val salt = generateSalt
      F.createUser(
        Username(login.username.value),
        Salt(salt),
        PwdHash((salt + login.password.value).bcrypt)
      )
    }

    override def authenticate(login: Login): F[Either[ErrorMsg, Token]] = {
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

  object UserServiceInterpreter {
    def apply[F[_]: Sync: UserDao](implicit crypto: CryptoBits) = new UserServiceInterpreter()
  }
}

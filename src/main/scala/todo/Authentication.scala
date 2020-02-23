package todo

import Model.{ErrorMsg, Login, Token, User}
import cats.Applicative
import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.{Clock, _}
import cats.implicits._
import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.rho.AuthedContext
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, Request}
import org.reactormonk.{CryptoBits, PrivateKey}
import todo.Algebras.UserDao
import com.github.t3hnar.bcrypt._
import User.Name.fromLoginUsername
import scala.concurrent.duration.MILLISECONDS

class Authentication[F[_]: Sync](dao: UserDao[F])(implicit secrets: Secrets)
    extends Http4sDsl[F]
    with CirceInstances
    with CirceEntityEncoder {




  def createuser(login: Login) = {
    val salt = generateSalt
    dao.create(
      User(
        User.Id(1), // todo, this is unused
        User.Name(login.username.value),
        User.Salt(salt),
        User.PwdHash((salt + login.password).bcrypt)
      )
    )
  }

  def issueToken(login: Login) = { // : F[Either[ErrorMsg, Token]] = {
    def validatePassword(password: Login.Password, hash: User.PwdHash, salt: User.Salt): Boolean =
      (salt.value + password.value).isBcrypted(hash.value)

    dao
      .findByName(login.username)
      .filter(user => validatePassword(login.password, user.pwdHash, user.salt))
      .semiflatMap { user =>
        clock
          .monotonic(MILLISECONDS)
          .map(time => Token(crypto.signToken(user.name.value.toString, time.toString)))
      }
      .toRight(ErrorMsg("Invalid Credentials"))
      .value

// alternative implementation, but I had to supply applicative to it explicitly because of diverging implicits

//    val token = for {
//      user <- dao.findByName(login.username)
//      if validatePassword(login.password, user.pwdHash, user.salt)
//      time <- OptionT.liftF(clock.monotonic(MILLISECONDS))
//      token <- OptionT.some(Token(crypto.signToken(user.id.value.toString, time.toString)))(Applicative[F])
//    } yield token
//
//    token.toRight(ErrorMsg("Invalid Credentials")).value

  }

  def middleware: AuthMiddleware[F, User] = {
    val authUser: Kleisli[F, Request[F], Either[ErrorMsg, User]] = Kleisli { request =>
      val message: Either[ErrorMsg, User.Id] = for {
        header <- request.headers.get(Authorization).toRight(ErrorMsg("Couldn't find an Authorization header"))
        token <- crypto.validateSignedToken(header.value).toRight(ErrorMsg("Invalid token"))
        message <- Either.catchOnly[NumberFormatException](token.toInt).bimap(err => ErrorMsg(err.toString), User.Id)
      } yield message

      message
        .map(
          dao
            .find(_)
            .map(_.toRight(ErrorMsg("user doesn't exist")))
        )
        .sequence
        .map(_.flatten)
    }

    val onFailure: AuthedRoutes[ErrorMsg, F] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))

    AuthMiddleware(authUser, onFailure)
  }

  def auth =
    Auth.auth // because auth.Auth.auth in routes would not be nice

  def toAuthedRoutes =
    Auth.toService(_)

  private val crypto =
    CryptoBits(
      PrivateKey(
        scala.io.Codec.toUTF8(secrets.encryptionKey)
      )
    )

  private val clock = Clock.create

  private object Auth extends AuthedContext[F, User]
}

object Authentication {
  def apply[F[_]: Sync](dao: UserDao[F])(implicit secrets: Secrets) = new Authentication(dao)
}

package todo

import java.util.UUID

import Model.{ErrorMsg, Login, Token, User}
import cats.data.{Kleisli, OptionT}
import cats.effect.{Clock, _}
import cats.implicits._
import org.http4s
import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.rho.AuthedContext
import org.http4s.rho.bits.TypedHeader
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes, Request, ResponseCookie}
import org.reactormonk.{CryptoBits, PrivateKey}
import shapeless.HNil
import todo.Algebras.{TodoDao, UserDao}

import scala.concurrent.duration.MILLISECONDS

class Authentication[F[_]: Sync](dao: UserDao[F])(implicit secrets: Secrets)
    extends Http4sDsl[F]
    with CirceInstances
    with CirceEntityEncoder {

  val dummyUser: F[User] = ???

  val key    = PrivateKey(scala.io.Codec.toUTF8(secrets.encryptionKey))
  val crypto = CryptoBits(key)
  val clock  = Clock.create

  val authUser: Kleisli[F, Request[F], Either[String, User]] = Kleisli { request =>
    val message: Either[String, User.Id] = for {
      header <- request.headers.get(Authorization).toRight("Couldn't find an Authorization header")
      token <- crypto.validateSignedToken(header.value).toRight("Invalid token")
      message <- Either.catchOnly[NumberFormatException](token.toInt).bimap(_.toString, User.Id)
    } yield message
    message
      .map(
        dao
          .find(_)
          .map(_.toRight("invalid token"))
      )
      .sequence
      .map(_.flatten)
  }

  def issueToken(login: Login): F[Either[ErrorMsg, Token]] = {

    for {
      user <- dao.findByName(login.username)
      time <- clock.monotonic(MILLISECONDS)
    } yield user
      .map((user: User) => Token(crypto.signToken(user.id.value.toString, time.toString)))
      .toRight(ErrorMsg("Invalid credentials"))

  }

  val onFailure: AuthedRoutes[String, F] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))

  val middleware = AuthMiddleware(authUser, onFailure)

  object Auth extends AuthedContext[F, User]

  def auth =
    Auth.auth // because auth.Auth.auth in routes would not be nice

  def toService =
    Auth.toService(_)

}

object Authentication {
  def apply[F[_]: Sync](dao: UserDao[F])(implicit secrets: Secrets) = new Authentication(dao)
}

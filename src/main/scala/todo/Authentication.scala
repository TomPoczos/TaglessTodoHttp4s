package todo

import Model.User
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
      .map(dao.find(_)
        .map(_.toRight("invalid token")))
      .sequence
      .map(_.flatten)
  }
  def verifyLogin(request: Request[F]): F[Either[String, User]] = ??? // gotta figure out how to do the form

  def getResp(request: Request[F], time: String): F[http4s.Response[F]] =
    verifyLogin(request).flatMap {
      case Left(error) =>
        Forbidden(error)
      case Right(user) =>
        val message = crypto.signToken(user.id.value.toString, time)
        Ok("Logged in!").map(_.addCookie(ResponseCookie("authcookie", message)))
    }

  val logIn: Kleisli[F, Request[F], http4s.Response[F]] = Kleisli { request =>
    for {
      time <- clock.monotonic(MILLISECONDS).map(_.toString)
      resp <- getResp(request, time)
    } yield resp
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

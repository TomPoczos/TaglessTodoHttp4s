package todo

import _root_.Model.ErrorResponse
import cats.Applicative
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.implicits._
import org.http4s.circe.CirceEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpRoutes, Request}

// at this point it might not be worth keeping this class and moveing what remains here to main...

class Server[F[_]: ConcurrentEffect: ContextShift: Timer](routes: HttpRoutes[F])(implicit config: HttpServerConig)
    extends Http4sDsl[F]
    with CirceEntityEncoder {

  val resource: Resource[F, org.http4s.server.Server[F]] =
    BlazeServerBuilder[F]
      .bindHttp(config.port, config.host)
      .withHttpApp(routes.orNotFound)
      .withServiceErrorHandler(errorHandler(_))
      .resource

  private def errorHandler(request: Request[F]): PartialFunction[Throwable, F[org.http4s.Response[F]]] = {
    case ex: Throwable =>
      Applicative[F].pure(println(s"UNHANDLED: ${ex}\n${ex.getStackTrace.mkString("\n")}")) *>
        InternalServerError(ErrorResponse("Something went wrong"))
  }
}

object Server {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](routes: HttpRoutes[F])(implicit config: HttpServerConig) =
    new Server(routes)
}

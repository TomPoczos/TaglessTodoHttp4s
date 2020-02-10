package todo

import _root_.Model.ErrorResponse
import cats.Applicative
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.implicits._
import org.http4s.circe.CirceEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpRoutes, Request}

class Server[F[_]: ConcurrentEffect: ContextShift: Timer](routes: HttpRoutes[F])(implicit config: HttpServerConig)
    extends Http4sDsl[F]
    with CirceEntityEncoder {

  def run: Resource[F, org.http4s.server.Server[F]] = {
    // NOTE: the import is necessary to get .orNotFound but clashes with a lot of rho names that's why it's imported inside the method
    import org.http4s.implicits._
    BlazeServerBuilder[F]
      .bindHttp(config.port, config.host)
      .withHttpApp(routes.orNotFound)
      .withServiceErrorHandler(errorHandler(_))
      .resource
//      .use(_ => Applicative[F].pure(Unit))
  }

  def errorHandler(request: Request[F]): PartialFunction[Throwable, F[org.http4s.Response[F]]] = {
    case ex: Throwable =>
      Applicative[F].pure(println(s"UNHANDLED: ${ex}\n${ex.getStackTrace.mkString("\n")}")) *>
        InternalServerError(ErrorResponse("Something went wrong"))
  }
}

object Server {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](routes: HttpRoutes[F])(implicit config: HttpServerConig) =
    new Server(routes)
}

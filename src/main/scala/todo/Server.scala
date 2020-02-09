package todo

import _root_.Model.{CreateTodo, ErrorResponse}
import cats.Applicative
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import org.http4s.circe.CirceEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.{DefaultSwaggerFormats, SwaggerSupport}
import org.http4s.rho.{RhoMiddleware, RhoRoutes}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.staticcontent.{FileService, fileService}
import org.http4s.{HttpRoutes, Request}

import scala.reflect.runtime.universe.typeOf

class Server[F[_]:ConcurrentEffect](routes: RhoRoutes[F]) extends Http4sDsl[F] with CirceEntityEncoder {

  val todoApiInfo = Info(
    title   = "TODO API",
    version = "0.1.0"
  )
  val host     = "localhost"
  val port     = 8080
  val basePath = "/v1"

  def run()(implicit cs: ContextShift[F], t: Timer[F]): F[Unit] = {
    // NOTE: the import is necessary to get .orNotFound but clashes with a lot of rho names that's why it's imported inside the method
    import org.http4s.implicits._
    BlazeServerBuilder[F]
      .bindHttp(port, host)
      .withHttpApp(createRoutes.orNotFound)
      .withServiceErrorHandler(errorHandler(_))
      .resource
      .use(_ => Applicative[F].pure(Unit))
  }

  def errorHandler(request: Request[F]): PartialFunction[Throwable, F[org.http4s.Response[F]]] = {
    case ex: Throwable =>
      Applicative[F].pure(println(s"UNHANDLED: ${ex}\n${ex.getStackTrace.mkString("\n")}")) *>
        InternalServerError(ErrorResponse("Something went wrong"))
  }

  def createRoutes(implicit cs: ContextShift[F]): HttpRoutes[F] =
    Router(
      "/docs"  -> fileService[F](FileService.Config[F]("./swagger")),
      basePath -> routes.toRoutes(swaggerMiddleware)
    )

  val swaggerMiddleware: RhoMiddleware[F] =
    SwaggerSupport
      .apply[F]
      .createRhoMiddleware(
        swaggerFormats = DefaultSwaggerFormats
          .withSerializers(typeOf[CreateTodo], CreateTodo.SwaggerModel),
        apiInfo  = todoApiInfo,
        host     = Some(s"${host}:${port}"),
        schemes  = List(Scheme.HTTP),
        basePath = Some(basePath),
        security = List(SecurityRequirement("Bearer", List())),
        securityDefinitions = Map(
          "Bearer" -> ApiKeyAuthDefinition("Authorization", In.HEADER)
        )
      )
}

object Server {
  def apply[F[_]:ConcurrentEffect](routes: RhoRoutes[F]) = new Server(routes)
}

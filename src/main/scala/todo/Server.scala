package todo

import _root_.Model.{CreateTodo, ErrorResponse}
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import org.http4s.circe.CirceEntityEncoder
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.{DefaultSwaggerFormats, SwaggerSupport}
import org.http4s.rho.{RhoMiddleware, RhoRoutes}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.staticcontent.{FileService, fileService}
import org.http4s.{HttpRoutes, Request}

import scala.reflect.runtime.universe.typeOf

class Server(routes: RhoRoutes[IO]) {

  val todoApiInfo = Info(
    title   = "TODO API",
    version = "0.1.0"
  )
  val host     = "localhost"
  val port     = 8080
  val basePath = "/v1"



  object ErrorHandler extends CirceEntityEncoder {
    // NOTE: This import clashes with a lot of rho names hence the wrapper object

    import org.http4s.dsl.io._

    def apply(request: Request[IO]): PartialFunction[Throwable, IO[org.http4s.Response[IO]]] = {
      case ex: Throwable =>
        IO(println(s"UNHANDLED: ${ex}\n${ex.getStackTrace.mkString("\n")}")) *>
          InternalServerError(ErrorResponse("Something went wrong"))
    }
  }

  def run()(implicit cs: ContextShift[IO], t: Timer[IO]): IO[Unit] = {
    // NOTE: the import is necessary to get .orNotFound but clashes with a lot of rho names that's why it's imported inside the method
    import org.http4s.implicits._
    BlazeServerBuilder[IO]
      .bindHttp(port, host)
      .withHttpApp(createRoutes.orNotFound)
      .withServiceErrorHandler(ErrorHandler(_))
      .resource
      .use(_ => IO.never)
  }

  def createRoutes(implicit cs: ContextShift[IO]): HttpRoutes[IO] =
    Router(
      "/docs"  -> fileService[IO](FileService.Config[IO]("./swagger")),
      basePath -> routes.toRoutes(swaggerMiddleware)
    )

  val swaggerMiddleware: RhoMiddleware[IO] =
    SwaggerSupport
      .apply[IO]
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
  def apply(routes: RhoRoutes[IO]) = new Server(routes)
}

package todo

import Model._
import cats._
import cats.effect.{ConcurrentEffect, ContextShift}
import cats.implicits._
import org.http4s._
import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.{DefaultSwaggerFormats, SwaggerSupport, SwaggerSyntax}
import org.http4s.rho.{RhoMiddleware, RhoRoutes}
import org.http4s.server._
import org.http4s.server.staticcontent.{fileService, FileService}
import todo.Algebras.TodoDao

import scala.reflect.runtime.universe.typeOf

class Routes[F[+_]: ConcurrentEffect: ContextShift](auth: Authentication[F], dao: TodoDao[F])(
    implicit config: HttpServerConig with ApiInfoConfig
) extends SwaggerSyntax[F]
    with CirceInstances
    with CirceEntityEncoder {

  private val todoRoutes: RhoRoutes[F] =
    new RhoRoutes[F]() {
      // ----------------------------------------------------------------------------------------------------------------------- //
      //  NOTE: If you run into issues with divergent implicits check out this issue https://github.com/http4s/rho/issues/292   //
      // ---------------------------------------------------------------------------------------------------------------------- //

      GET / "todo" >>> auth.auth |>> { user: User =>
        dao
          .findAll()
          .flatMap(Ok(_))
      }

      POST / "todo" >>> auth.auth ^ jsonOf[F, CreateTodo] |>> { (user: User, createTodo: CreateTodo) =>
        dao
          .create(createTodo.name)
          .flatMap(_ => Ok(EmptyResponse()))
      }

      POST / "todo" / pathVar[Int] >>> auth.auth |>> { (todoId: Int, user: User) =>
        dao.markAsDone(todoId).flatMap {
          case 0 => NotFound(ErrorResponse(s"Todo with id: `${todoId}` not found"))
          case 1 => Ok(EmptyResponse())
          case _ =>
            Applicative[F].pure(println(s"Inconsistent data: More than one todo updated in POST /todo/${todoId}")) *>
              InternalServerError(ErrorResponse("Ooops, something went wrong..."))
        }
      }
    }

  private val userRoutes: RhoRoutes[F] =
    new RhoRoutes[F] {

      "Login" **
        POST / "auth" ^ jsonOf[F, Login] |>> { login: Login =>
        auth
          .issueToken(login)
          .flatMap {
            case Left(error) =>
              Forbidden(error.value)
            case Right(token) =>
              Ok("Logged in!").map(_.addCookie(ResponseCookie("authcookie", token.value)))
          }
      }

      "Create a new user" **
        POST / "auth" / "new" ^ jsonOf[F, Login] |>> { login: Login =>
      }
    }

  private val swaggerMiddleware: RhoMiddleware[F] =
    SwaggerSupport
      .apply[F]
      .createRhoMiddleware(
        swaggerFormats = DefaultSwaggerFormats
          .withSerializers(typeOf[CreateTodo], CreateTodo.SwaggerModel),
        apiInfo  = config.todoApiInfo,
        host     = Some(s"${config.host}:${config.port}"),
        schemes  = List(Scheme.HTTP),
        basePath = Some(config.basePath),
        security = List(SecurityRequirement("Bearer", List())),
        securityDefinitions = Map(
          "Bearer" -> ApiKeyAuthDefinition("Authorization", In.HEADER)
        )
      )

  private val router = Router[F](
    "/docs" -> fileService[F](FileService.Config[F]("./swagger")),
    config.basePath ->
      auth.middleware(
        auth.toAuthedRoutes(
          (todoRoutes and userRoutes).toRoutes(swaggerMiddleware)
        )
      )
  )
}

object Routes {

  def apply[F[+_]: ConcurrentEffect: ContextShift](
      auth:          Authentication[F],
      dao:           TodoDao[F]
  )(implicit config: HttpServerConig with ApiInfoConfig): HttpRoutes[F] = {
    new Routes(auth, dao).router
  }
}

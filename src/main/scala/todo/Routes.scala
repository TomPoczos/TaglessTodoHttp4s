package todo

import Model.{CreateTodo, EmptyResponse, ErrorResponse, Login}
import cats.{Applicative, Monad}
import cats.effect.{ConcurrentEffect, ContextShift, IO, Sync}
import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
import org.http4s.rho.{RhoMiddleware, RhoRoutes}
import org.http4s.rho.swagger.{DefaultSwaggerFormats, SwaggerSupport, SwaggerSyntax}
import Algebras.TodoDao
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.rho.swagger.models.{ApiKeyAuthDefinition, In, Info, Scheme, SecurityRequirement}
import org.http4s.server.Router
import org.http4s.server.staticcontent.{FileService, fileService}
import scala.reflect.runtime.universe.typeOf

class Routes[F[+_]:ConcurrentEffect:Sync:ContextShift](dao: TodoDao[F]) {

  val todoApiInfo = Info(
    title   = "TODO API",
    version = "0.1.0"
  )
  val host     = "localhost"
  val port     = 8080
  val basePath = "/v1"

  val todoRoutes: RhoRoutes[F] =
    new RhoRoutes[F] with SwaggerSyntax[F] with CirceInstances with CirceEntityEncoder {
      // ----------------------------------------------------------------------------------------------------------------------- //
      //  NOTE: If you run into issues with divergent implicits check out this issue https://github.com/http4s/rho/issues/292   //
      // ---------------------------------------------------------------------------------------------------------------------- //

      GET / "todo" |>> { () =>
        dao
          .findAll()
          .flatMap(Ok(_))
      }

      POST / "todo" ^ jsonOf[F, CreateTodo] |>> { createTodo: CreateTodo =>
        dao
          .create(createTodo.name)
          .flatMap(_ => Ok(EmptyResponse()))
      }

      POST / "todo" / pathVar[Int] |>> { todoId: Int =>
        dao.markAsDone(todoId).flatMap {
          case 0 => NotFound(ErrorResponse(s"Todo with id: `${todoId}` not found"))
          case 1 => Ok(EmptyResponse())
          case _ =>
            Applicative[F].pure(println(s"Inconsistent data: More than one todo updated in POST /todo/${todoId}")) *>
              InternalServerError(ErrorResponse("Ooops, something went wrong..."))
        }
      }
    }

  val userRoutes: RhoRoutes[F] =
    new RhoRoutes[F] with SwaggerSyntax[F] with CirceInstances with CirceEntityEncoder {

      "Login" **
        POST / "auth" ^ jsonOf[F, Login] |>> { login: Login =>
        login.username
      }

      "Create a new user" **
        POST / "auth" / "new" ^ jsonOf[F, Login] |>> { login: Login =>
        //        login.username
      }
    }

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


object Routes {
  def apply[F[+_]:ConcurrentEffect:Sync:ContextShift](dao: TodoDao[F]): HttpRoutes[F] = {
    val routes = new Routes(dao)
    Router[F](
      "/docs"  -> fileService[F](FileService.Config[F]("./swagger")),
      routes.basePath -> (routes.todoRoutes and routes.userRoutes).toRoutes(routes.swaggerMiddleware)
    )
  }
}

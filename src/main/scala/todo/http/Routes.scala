package todo.http

import cats.effect.{ConcurrentEffect, ContextShift, Sync}
import org.http4s.headers.Authorization
import org.http4s.rho.swagger.models._
import todo.Model._
import cats.data.{Kleisli, OptionT}
import cats.implicits._
import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
import org.http4s.dsl.Http4sDsl
import org.http4s.rho.swagger.{DefaultSwaggerFormats, SwaggerSupport, SwaggerSyntax}
import org.http4s.rho.{RhoMiddleware, RhoRoutes}
import org.http4s.server.{AuthMiddleware, _}
import org.http4s.server.staticcontent.{fileService, FileService}
import org.http4s.{AuthedRoutes, Request, _}
import org.reactormonk.CryptoBits
import todo.dataaccess.Algebras.{TodoDao, UserDao}
import todo.Configuration.{ApiInfoConfig, HttpServerConfig}
import todo.services.Algebras.UserService

import scala.reflect.runtime.universe.typeOf
import org.http4s.rho.AuthedContext

class Routes[F[+_]: ConcurrentEffect: ContextShift: UserService: TodoDao: UserDao](
    implicit
    config: HttpServerConfig with ApiInfoConfig,
    crypto: CryptoBits
) extends SwaggerSyntax[F]
    with Http4sDsl[F]
    with CirceInstances
    with CirceEntityEncoder {

  private val todoRoutes: RhoRoutes[F] =
    new RhoRoutes[F]() {

      GET >>> Auth.auth |>> { user: User =>
        F.findTodos(user.id).flatMap(Ok(_))
      }

      POST >>> Auth.auth ^ jsonOf[F, CreateTodo] |>> { (user: User, createTodo: CreateTodo) =>
        F.createTodo(createTodo.name, user.id)
          .flatMap(_ => Ok(EmptyResponse()))
      }

      POST / pathVar[Int] >>> Auth.auth |>> { (todoId: Int, user: User) =>
        F.markAsDone(TodoId(todoId), user.id).flatMap {
          case 0 => NotFound(ErrorResponse(s"Todo with id: `${todoId}` not found"))
          case 1 => Ok(EmptyResponse())
          case _ =>
            F.delay(println(s"Inconsistent data: More than one todo updated in POST /todo/${todoId}")) *>
              InternalServerError(ErrorResponse("Ooops, something went wrong..."))
        }
      }
    }

  private val userRoutes: RhoRoutes[F] =
    new RhoRoutes[F] {

      "Login" **
        POST ^ jsonOf[F, todo.Model.Login] |>> { login: todo.Model.Login =>
        F.authenticate(login)
          .flatMap {
            case Left(error) =>
              Forbidden(error.value)
            case Right(token) =>
              Ok("Logged in!").map(_.addCookie(ResponseCookie("authcookie", token.value)))
          }
      }

      "Create a new user" **
        POST / "new" ^ jsonOf[F, Login] |>> { login: Login =>
        F.createuser(login)
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

  def middleware: AuthMiddleware[F, User] = {
    val authUser: Kleisli[F, Request[F], Either[ErrorMsg, User]] = Kleisli { request =>
      val message: Either[ErrorMsg, UserId] = for {
        header <- request.headers.get(Authorization).toRight(ErrorMsg("Couldn't find an Authorization header"))
        token <- crypto.validateSignedToken(header.value.split(' ')(1)).toRight(ErrorMsg("Invalid token"))
        message <- Either.catchOnly[NumberFormatException](token.toInt).bimap(err => ErrorMsg(err.toString), UserId)
      } yield message

      message
        .map(
          F.findUser(_)
            .map(_.toRight(ErrorMsg("user doesn't exist")))
        )
        .sequence
        .map(_.flatten)
    }

    val onFailure: AuthedRoutes[ErrorMsg, F] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))

    AuthMiddleware(authUser, onFailure)
  }

  private object Auth extends AuthedContext[F, User]

  val router = Router[F](
    "/docs"                   -> fileService[F](FileService.Config[F]("./swagger")),
    config.basePath + "/auth" -> userRoutes.toRoutes(swaggerMiddleware),
    config.basePath + "/todo" ->
      middleware(
        Auth.toService(
          todoRoutes.toRoutes(swaggerMiddleware)
        )
      )
  )
}

object Routes {

  def apply[F[+_]: ConcurrentEffect: ContextShift: UserService: TodoDao: UserDao](
      implicit
      config: HttpServerConfig with ApiInfoConfig,
      crypto: CryptoBits
  ): Routes[F] = new Routes
}

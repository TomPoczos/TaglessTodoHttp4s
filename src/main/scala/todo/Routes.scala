package todo

import Model.{CreateTodo, EmptyResponse, ErrorResponse, Login}
import cats.{Applicative, Monad}
import cats.effect.{ConcurrentEffect, IO, Sync}
import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.SwaggerSyntax
import Algebras.TodoDao
import cats.implicits._

class Routes[F[+_]:ConcurrentEffect](dao: TodoDao[F]) {

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
}

object Routes {
  def apply[F[+_]:ConcurrentEffect:Sync:Monad](dao: TodoDao[F]) = {
    val routes = new Routes(dao)
    routes.todoRoutes and routes.userRoutes
  }
}

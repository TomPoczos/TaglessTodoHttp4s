package todo

import Model.{CreateTodo, EmptyResponse, ErrorResponse, Login}
import cats.effect.IO
import org.http4s.circe.{CirceEntityEncoder, CirceInstances}
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.SwaggerSyntax
import todo.dataaccess.Algebras.TodoDao
import cats.implicits._

class Routes(dao: TodoDao[IO, List]) {

  val todoRoutes: RhoRoutes[IO] =
    new RhoRoutes[IO] with SwaggerSyntax[IO] with CirceInstances with CirceEntityEncoder {
      // ----------------------------------------------------------------------------------------------------------------------- //
      //  NOTE: If you run into issues with divergent implicits check out this issue https://github.com/http4s/rho/issues/292   //
      // ---------------------------------------------------------------------------------------------------------------------- //

      GET / "todo" |>> { () =>
        dao
          .findAll()
          .flatMap(Ok(_))
      }

      POST / "todo" ^ jsonOf[IO, CreateTodo] |>> { createTodo: CreateTodo =>
        dao
          .create(createTodo.name)
          .flatMap(_ => Ok(EmptyResponse()))
      }

      POST / "todo" / pathVar[Int] |>> { todoId: Int =>
        dao.markAsDone(todoId).flatMap {
          case 0 => NotFound(ErrorResponse(s"Todo with id: `${todoId}` not found"))
          case 1 => Ok(EmptyResponse())
          case _ =>
            IO(println(s"Inconsistent data: More than one todo updated in POST /todo/${todoId}")) *>
              InternalServerError(ErrorResponse("Ooops, something went wrong..."))
        }
      }
    }

  val userRoutes: RhoRoutes[IO] =
    new RhoRoutes[IO] with SwaggerSyntax[IO] with CirceInstances with CirceEntityEncoder {

      "Login" **
        POST / "auth" ^ jsonOf[IO, Login] |>> { login: Login =>
        login.username
      }

      "Create a new user" **
        POST / "auth" / "new" ^ jsonOf[IO, Login] |>> { login: Login =>
        //        login.username
      }
    }
}

object Routes {
  def apply(dao: TodoDao[IO, List]) = {
    val routes = new Routes(dao)
    routes.todoRoutes and routes.userRoutes
  }
}

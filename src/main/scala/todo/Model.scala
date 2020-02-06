package object todo

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.rho.swagger.models.{Model, ModelImpl, StringProperty}

package object Model {
  case class Todo(
                   id:   Int,
                   name: String,
                   done: Boolean
                 )

  object Todo {
    implicit val encoder = deriveEncoder[Todo]
    implicit val decoder = deriveDecoder[Todo]
  }

  case class CreateTodo(name: String)

  object CreateTodo {
    implicit val encoder = deriveEncoder[CreateTodo]
    implicit val decoder = deriveDecoder[CreateTodo]

    val SwaggerModel: Set[Model] = Set(
      ModelImpl(
        id          = "CreateTodo",
        id2         = "CreateTodo",
        `type`      = Some("object"),
        description = Some("CreateTodo description"),
        name        = Some("CreateTodo"),
        properties = Map(
          "name" -> StringProperty(
            required    = true,
            description = Some("name of the todo item"),
            enums       = Set()
          )
        ),
        example = Some("""{ "name" : "todo 1" }""")
      )
    )
  }

  case class EmptyResponse()

  object EmptyResponse {
    implicit val encoder = deriveEncoder[EmptyResponse]
    implicit val decoder = deriveDecoder[EmptyResponse]
  }

  case class ErrorResponse(message: String)

  object ErrorResponse {
    implicit val encoder = deriveEncoder[ErrorResponse]
    implicit val decoder = deriveDecoder[ErrorResponse]
  }

  case class Login(username: String, PassWord: String)

  object Login {
    implicit val encoder = deriveEncoder[Login]
    implicit val decoder = deriveDecoder[Login]
  }

}


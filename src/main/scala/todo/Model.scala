package object todo

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.rho.swagger.models.{Model, ModelImpl, StringProperty}

// considered full automatic derivation but probably prefer having all this boilerplate to remembering the auto import
// at each use site

// added some wrapper case classes to avoid the primitive obsession antipattern,
// although the extra boilerplate code and more complex json is a tradeoff not necessarily worth it in all cases.

// opted for nesting rather than hungarian notation-like prependings before generic class names

package object Model {

  case class Todo(id: Todo.Id, name: Todo.Name, done: Todo.Done)

  object Todo {

    case class Id(value: Int)

    object Id {
      implicit val encoder = deriveEncoder[Todo.Id]
      implicit val decoder = deriveDecoder[Todo.Id]
    }

    case class Name(value: String)

    object Name {
      implicit val encoder = deriveEncoder[Todo.Name]
      implicit val decoder = deriveDecoder[Todo.Name]
    }

    case class Done(value: Boolean)

    object Done {
      implicit val encoder = deriveEncoder[Todo.Done]
      implicit val decoder = deriveDecoder[Todo.Done]
    }

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

  case class Login(username: Login.Username, password: Login.Password)

  object Login {
    case class Username(value: String)

    object Username {
      implicit val encoder = deriveEncoder[Username]
      implicit val decoder = deriveDecoder[Username]
    }

    case class Password(value: String)

    object Password {
      implicit val encoder = deriveEncoder[Password]
      implicit val decoder = deriveDecoder[Password]
    }

    implicit val encoder = deriveEncoder[Login]
    implicit val decoder = deriveDecoder[Login]
  }
}

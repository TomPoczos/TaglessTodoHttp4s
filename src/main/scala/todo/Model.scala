package object todo

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.rho.swagger.models.{Model, ModelImpl, StringProperty}

package object Model {

  case class Todo(
      id:   Todo.Id,
      name: Todo.Name,
      done: Todo.Done
  )

  object Todo {

    case class Id(value:   Int)
    case class Name(value: String)
    case class Done(value: Boolean)

    implicit val todoDecoder: Decoder[Todo] =
      (c: HCursor) =>
        for {
          id <- c.downField("id").as[Int]
          name <- c.downField("name").as[String]
          done <- c.downField("done").as[Boolean]
        } yield Todo(Id(id), Name(name), Done(done))

    implicit val todoEncoder: Encoder[Todo] =
      (todo: Todo) =>
        Json.obj(
          ("id", Json.fromInt(todo.id.value)),
          ("name", Json.fromString(todo.name.value)),
          ("done", Json.fromBoolean(todo.done.value))
        )
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
    case class Password(value: String)

    implicit val loginDecoder: Decoder[Login] =
      (c: HCursor) =>
        for {
          username <- c.downField("username").as[String]
          password <- c.downField("password").as[String]
        } yield Login(Username(username), Password(password))

    implicit val loginEncoder: Encoder[Login] =
      (login: Login) =>
        Json.obj(
          ("username", Json.fromString(login.username.value)),
          ("password", Json.fromString(login.password.value))
        )
  }

  case class User(
      id:      User.Id,
      name:    User.Name,
      salt:    User.Salt,
      pwdHash: User.PwdHash
  )

  object User {
    case class Id(value:   Int)
    case class Name(value: String)

    object Name {
      implicit def fromLoginUsername(name: Login.Username) =
        User.Name(name.value)
    }

    case class Salt(value:    String)
    case class PwdHash(value: String)
  }

  case class Authenticated(value: Boolean)

  case class Token(value: String)

  case class ErrorMsg(value: String)
  object ErrorMsg {
    implicit val encoder = deriveEncoder[ErrorMsg]
    implicit val decoder = deriveDecoder[ErrorMsg]
  }
}

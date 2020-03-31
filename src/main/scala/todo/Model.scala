package todo

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.rho.swagger.models.{Model, ModelImpl, StringProperty}

package object Model {

  case class TodoId(value:   Int)

  case class TodoName(value: String)
  
  case class TodoDone(value: Boolean)

  case class Todo(
      id:   TodoId,
      name: TodoName,
      done: TodoDone
  )

  object Todo {
    implicit val todoDecoder: Decoder[Todo] =
      (c: HCursor) =>
        for {
          id <- c.downField("id").as[Int]
          name <- c.downField("name").as[String]
          done <- c.downField("done").as[Boolean]
        } yield Todo(TodoId(id), TodoName(name), TodoDone(done))

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

  case class Password(value: String)

  case class Login(username: Username, password: Password)

  object Login {

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

  case class UserId(value:   Int)

  case class Salt(value:     String)

  case class PwdHash(value:  String)

  case class Username(value: String)

  case class User(
      id:      UserId,
      name:    Username,
      salt:    Salt,
      pwdHash: PwdHash
  )

  case class Authenticated(value: Boolean)

  case class Token(value: String)

  case class ErrorMsg(value: String)
  
  object ErrorMsg {
    implicit val encoder = deriveEncoder[ErrorMsg]
    implicit val decoder = deriveDecoder[ErrorMsg]
  }
}

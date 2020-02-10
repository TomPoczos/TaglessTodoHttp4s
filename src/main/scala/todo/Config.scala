package todo

import org.http4s.rho.swagger.models.Info

trait HttpServerConig {
  val host     = "localhost"
  val port     = 8080
  val basePath = "/v1"
}

trait ApiInfoConfig {
  val todoApiInfo = Info(
    title   = "TODO API",
    version = "0.1.0"
  )
}

trait DbConfig {
  val dbDriver = "org.sqlite.JDBC"
  val dbUrl = "jdbc:sqlite:todo.db"
}

object Config extends HttpServerConig with ApiInfoConfig with DbConfig

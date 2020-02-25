package todo

import org.http4s.rho.swagger.models.Info
import todo.Configuration.{ApiInfoConfig, DbConfig, HttpServerConfig}

package object Configuration {


  trait HttpServerConfig {
    val host = "localhost"
    val port = 8080
    val basePath = "/v1"
  }

  trait ApiInfoConfig {
    val todoApiInfo = Info(
      title = "TODO API",
      version = "0.1.0"
    )
  }

  trait DbConfig {
    val dbDriver = "org.sqlite.JDBC"
    val dbUrl = "jdbc:sqlite:todo.db"
  }
}

object Config extends HttpServerConfig with ApiInfoConfig with DbConfig with Secrets

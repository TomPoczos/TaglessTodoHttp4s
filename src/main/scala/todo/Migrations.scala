package todo

import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.Transactor

object Migrations {

  val migrations =
    List(
      sql"""
           |PRAGMA foreign_keys = ON
           |""",
      sql"""
           |create table if not exists user(
           |  id integer primary key,
           |  name text,
           |  salt text,
           |  pwdHash text not null
           |)
           |""",
      sql"""
           |create table if not exists todo(
           |  id integer primary key,
           |  user_fk integer references user(id),
           |  name text not null,
           |  done tinyint not null default 0
           |)
           |""",
    )

  def run(transactor: Transactor[IO]): IO[List[Int]] =
    migrations.traverse {
      _.stripMargin.update.run.transact(transactor)
    }
}

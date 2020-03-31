package todo.dataaccess.interpreters

import Model._
import cats.implicits._
import cats.data.OptionT
import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import todo.dataaccess.Algebras._

class MigrationsInterpreter[F[_]: Sync: Transactor] {

  def run: F[List[Int]] =
    List(
      sql"""
           |PRAGMA foreign_keys = ON
           |""",
      sql"""
           |create table if not exists user(
           |  id integer primary key,
           |  name text not null unique,
           |  salt text not null unique,
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
           |"""
    ).traverse(_.stripMargin.update.run.transact(F))
}

object MigrationsInterpreter {
  def apply[F[_]: Sync: Transactor] = new MigrationsInterpreter[F]
}

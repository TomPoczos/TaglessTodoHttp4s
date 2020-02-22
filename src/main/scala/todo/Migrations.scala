package todo

import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.Transactor

class Migrations[F[_]:Sync](transactor: Transactor[F]) {

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
    ).traverse(_.stripMargin.update.run.transact(transactor))
}

object Migrations {
  def apply[F[_]:Sync](transactor: Transactor[F]) =
    new Migrations(transactor)
}

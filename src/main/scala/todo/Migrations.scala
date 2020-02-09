package todo

import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.Transactor

object Migrations {

  val migrations =
    List(
      sql"""
         |create table if not exists todo(
         | id integer primary key,
         | name text not null,
         | done tinyint not null default 0
         |)
         |"""
    )

  def run(transactor: Transactor[IO]): IO[List[Int]] =
      migrations
        .map(_.stripMargin.update.run.transact(transactor))
        .sequence
}

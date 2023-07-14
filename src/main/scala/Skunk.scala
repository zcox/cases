package main

import cats.effect.{Async, Resource}
import skunk._
import natchez.Trace.Implicits.noop
import cats.effect.std.Console

object Skunk {

  def messageDbSessionPool[F[_]: Async: Console](): Resource[F, Resource[F, Session[F]]] =
    Session.pooled[F](
      host = "localhost",
      port = 5432,
      user = "postgres",
      database = "message_store",
      password = Some("postgres"),
      max = 10,
      parameters = Map(
        //messagedb's tables etc are in the message_store schema, not public schema
        "search_path" -> "message_store", 
        //http://docs.eventide-project.org/user-guide/message-db/server-functions.html#filtering-messages-with-a-sql-condition
        "message_store.sql_condition" -> "on"
      ) ++ Session.DefaultConnectionParameters,
    )

  def viewSessionPool[F[_]: Async: Console](): Resource[F, Resource[F, Session[F]]] =
    Session.pooled[F](
      host = "localhost",
      port = 5433,
      user = "postgres",
      database = "postgres",
      password = Some("postgres"),
      max = 10,
    )

}

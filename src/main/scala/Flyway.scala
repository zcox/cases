package main

import cats.syntax.all._
import cats.effect.Sync
import org.flywaydb.core.{Flyway => JFlyway}

object Flyway {

  def migrate[F[_]: Sync](
    host: String, 
    port: Int, 
    database: String, 
    user: String, 
    password: String
  ): F[Unit] = 
    Sync[F].delay(
      JFlyway
        .configure()
        .dataSource(s"jdbc:postgresql://$host:$port/$database?user=$user&password=$password", "sa", null)
        .load()
        .migrate()
      ).void
}

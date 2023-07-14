package watching

import java.util.UUID
import java.time.{ZonedDateTime, OffsetDateTime}
import cats._
import cats.syntax.all._
import cats.effect._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import cats.effect.Resource
import messagedb._
import fs2.Stream

trait Read[F[_]] {
  def watchers(caseId: UUID): Stream[F, UUID]
}

object Read {

  object Watchers {
    val query: Query[UUID, UUID] =
      sql"""
        SELECT user_id FROM watching WHERE case_id = $uuid
      """.query(uuid)
  }

  def fromPool[F[_]: Concurrent](pool: Resource[F, Session[F]]): Read[F] =
    new Read[F] {

      //TODO extract
      private def prepareResource[A, B](query: Query[A, B]): Resource[F, PreparedQuery[F, A, B]] =
        pool.flatMap(_.prepare(query))

      //TODO extract
      private def prepareStream[A, B](query: Query[A, B]): Stream[F, PreparedQuery[F, A, B]] =
        Stream.resource(prepareResource(query))

      //TODO hard-coded chunk size of 64
      override def watchers(caseId: UUID): Stream[F, UUID] =
        prepareStream(Watchers.query)
          .flatMap(_.stream(caseId, 64))
    }
}

trait Write[F[_]] {
  def watch(
    caseId: UUID,
    userId: UUID,
    createdAt: ZonedDateTime,
  ): F[Unit]

  def unwatch(
    caseId: UUID,
    userId: UUID,
  ): F[Unit]
}

object Write {

  //this is idempotent because of ON CONFLICT DO NOTHING
  object Watch {
    val command: Command[UUID ~ UUID ~ OffsetDateTime] = 
      sql"""
        INSERT INTO
          watching (case_id, user_id, created_at)
        VALUES
          ($uuid, $uuid, $timestamptz)
        ON CONFLICT DO NOTHING
      """.command
  }

  //this is idempotent because multiple deletes of same caseId/userId result in absence of the row
  object Unwatch {
    val command: Command[UUID ~ UUID] = 
      sql"""
        DELETE FROM
          watching
        WHERE
          case_id = $uuid AND user_id = $uuid
      """.command
  }

  def fromPool[F[_]: MonadCancelThrow](pool: Resource[F, Session[F]]): Write[F] = 
    new Write[F] {

      //TODO extract
      private def prepareResource[A](command: Command[A]): Resource[F, PreparedCommand[F, A]] =
        pool.flatMap(_.prepare(command))

      override def watch(
        caseId: UUID,
        userId: UUID,
        createdAt: ZonedDateTime,
      ): F[Unit] = 
        prepareResource(Watch.command)
          .use(_.execute(caseId ~ userId ~ createdAt.toOffsetDateTime).void)

      override def unwatch(
        caseId: UUID,
        userId: UUID,
      ): F[Unit] = 
        prepareResource(Unwatch.command)
          .use(_.execute(caseId ~ userId).void)

    }

}

case class Aggregator[F[_]](write: Write[F])(implicit F: MonadError[F, Throwable]) {

  private def watched(event: Watched): F[Unit] =
    write.watch(
      caseId = event.caseId,
      userId = event.userId,
      createdAt = event.timestamp,
    )

  private def unwatched(event: Unwatched): F[Unit] =
    write.unwatch(
      caseId = event.caseId,
      userId = event.userId,
    )

  def handle(event: MessageDb.Read.Message): F[Unit] =
    event.`type` match {
      case "Watched" => 
        for {
          data <- event.dataAs[Watched].liftTo[F]
          () <- watched(data)
        } yield ()
      case "Unwatched" => 
        for {
          data <- event.dataAs[Unwatched].liftTo[F]
          () <- unwatched(data)
        } yield ()
      case _ => Applicative[F].unit
    }
}

object Aggregator {
  def apply[F[_]: Temporal](mdb: MessageDb[F], write: Write[F]): Stream[F, Unit] = {
    val a = Aggregator[F](write)
    mdb.subscribe(
      category = Event.category,
      subscriberId = "aggregators:watching:v1",
      f = a.handle(_),
    )
  }
}

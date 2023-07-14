package cases

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
import io.circe.generic.semiauto._

trait Read[F[_]] {
  def getCasesByTenantId(tenantId: UUID): Stream[F, Read.Case]
}

object Read {

  case class Case(
    caseId: UUID,
    tenantId: UUID,
    creatorId: UUID,
    title: String,
    description: String,
    createdAt: OffsetDateTime,
  )

  object Case {
    val codec = uuid ~ uuid ~ uuid ~ text ~ text ~ timestamptz
    val decoder = codec.gmap[Case]

    implicit val encoder: io.circe.Encoder[Case] = deriveEncoder
  }

  object GetCasesByTenantId {
    val query: Query[UUID, Case] =
      sql"""
        SELECT case_id, tenant_id, creator_id, title, description, created_at FROM cases WHERE tenant_id = $uuid ORDER BY created_at DESC
      """.query(Case.decoder)
  }

  def fromPool[F[_]: Concurrent](pool: Resource[F, Session[F]]): Read[F] =
    new Read[F] {

      private def prepareResource[A, B](query: Query[A, B]): Resource[F, PreparedQuery[F, A, B]] =
        pool.flatMap(_.prepare(query))

      private def prepareStream[A, B](query: Query[A, B]): Stream[F, PreparedQuery[F, A, B]] =
        Stream.resource(prepareResource(query))

      //TODO hard-coded chunk size of 64
      override def getCasesByTenantId(tenantId: UUID): Stream[F, Read.Case] =
        prepareStream(GetCasesByTenantId.query)
          .flatMap(_.stream(tenantId, 64))
    }
}

trait Write[F[_]] {
  def addCase(
    caseId: UUID,
    tenantId: UUID,
    creatorId: UUID,
    title: String,
    createdAt: ZonedDateTime,
  ): F[Unit]

  def changeDescription(
    caseId: UUID,
    description: String,
  ): F[Unit]
}

object Write {

  //this is idempotent because of ON CONFLICT DO NOTHING
  object AddCase {
    val command: Command[UUID ~ UUID ~ UUID ~ String ~ OffsetDateTime] = 
      sql"""
        INSERT INTO
          cases (case_id, tenant_id, creator_id, title, created_at)
        VALUES
          ($uuid, $uuid, $uuid, $text, $timestamptz)
        ON CONFLICT DO NOTHING
      """.command
  }

  //this is idempotent because it's a key-value put
  object ChangeDescription {
    val command: Command[String ~ UUID] = 
      sql"""
        UPDATE
          cases
        SET
          description = $text
        WHERE
          case_id = $uuid
      """.command
  }

  def fromPool[F[_]: MonadCancelThrow](pool: Resource[F, Session[F]]): Write[F] = 
    new Write[F] {

      private def prepareResource[A](command: Command[A]): Resource[F, PreparedCommand[F, A]] =
        pool.flatMap(_.prepare(command))

      override def addCase(
        caseId: UUID,
        tenantId: UUID,
        creatorId: UUID,
        title: String,
        createdAt: ZonedDateTime,
      ): F[Unit] = 
        prepareResource(AddCase.command)
          .use(_.execute(caseId ~ tenantId ~ creatorId ~ title ~ createdAt.toOffsetDateTime).void)

      override def changeDescription(
        caseId: UUID,
        description: String,
      ): F[Unit] = 
        prepareResource(ChangeDescription.command)
          .use(_.execute(description ~ caseId).void)
    }

}

case class Aggregator[F[_]](write: Write[F])(implicit F: MonadError[F, Throwable]) {

  private def started(event: Started): F[Unit] =
    write.addCase(
      caseId = event.caseId,
      tenantId = event.tenantId,
      creatorId = event.creatorId,
      title = event.title,
      createdAt = event.timestamp,
    )

  private def descriptionChanged(event: DescriptionChanged): F[Unit] = 
    write.changeDescription(
      caseId = event.caseId,
      description = event.description,
    )

  def handle(event: MessageDb.Read.Message): F[Unit] =
    event.`type` match {
      case "Started" => 
        for {
          data <- event.dataAs[Started].liftTo[F]
          () <- started(data)
        } yield ()
      case "DescriptionChanged" => 
        for {
          data <- event.dataAs[DescriptionChanged].liftTo[F]
          () <- descriptionChanged(data)
        } yield ()
      case _ => Applicative[F].unit
    }
}

object Aggregator {
  def apply[F[_]: Temporal](mdb: MessageDb[F], write: Write[F]): Stream[F, Unit] = {
    val a = Aggregator[F](write)
    mdb.subscribe(
      category = "cases",
      subscriberId = "aggregators:cases",
      f = a.handle(_),
    )
  }
}

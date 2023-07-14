package cases

import cats._
import cats.syntax.all._
import cats.effect._
import java.util.UUID
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import messagedb._
import io.circe.syntax._

//TODO idempotency?

object StartHandler {

  def start(
    command: Start, 
    timestamp: ZonedDateTime
  ): Either[Throwable, Started] = 
    if (command.title.trim.isEmpty)
      new IllegalArgumentException(show"Title must not be empty").asLeft
    else
      Started(
        tenantId = command.tenantId,
        caseId = command.caseId,
        title = command.title,
        creatorId = command.creatorId,
        timestamp = timestamp,
      ).asRight

}

case class StartHandler[F[_]: Clock](
  messageDb: MessageDb[F],
)(implicit F: MonadError[F, Throwable]) {
  import StartHandler._

  def now: F[ZonedDateTime] = 
    Clock[F].realTime.map(d => Instant.ofEpochMilli(d.toMillis).atZone(ZoneOffset.UTC))

  def write(event: Started): F[Unit] = 
    messageDb.writeMessage(
      //TODO should our event case classes have an eventId field?
      id = UUID.randomUUID(),
      streamName = Event.streamName(event.caseId),
      `type` = event.getClass.getSimpleName,
      data = event.asJson,
      //TODO what should we put in metadata?
      metadata = none,
      //ensures this event is the first in the stream, error otherwise
      expectedVersion = -1L.some,
    ).void

  def handle(command: Start): F[Unit] = 
    for {
      ts <- now
      event <- start(command, ts).liftTo[F]
      () <- write(event)
    } yield ()
}

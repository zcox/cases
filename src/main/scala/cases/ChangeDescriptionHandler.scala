package cases

import cats.syntax.all._
import cats.effect._
import java.util.UUID
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import messagedb._
import io.circe.syntax._

//TODO idempotency?

object ChangeDescriptionHandler {

  case class Case(
    description: String,
    version: Long,
  )

  object Case {

    def update(o: Option[Case], m: MessageDb.Read.Message): Option[Case] = 
      m.`type` match {
        case "Started" =>
          Case("", m.position).some
        case "DescriptionChanged" =>
          //TODO what should we do if data isn't decodable to DescriptionChanged?
          m
            .dataAs[DescriptionChanged]
            .toOption
            .flatMap(e => 
              o.map(_.copy(
                description = e.description, 
                version = m.position
              ))
            )
        case _ => 
          o.map(_.copy(version = m.position))
      }

  }

  def changeDescription(
    command: ChangeDescription,
    maybeState: Option[Case],
    timestamp: ZonedDateTime,
  ): Either[Throwable, DescriptionChanged] = 
    maybeState
      .toRight(new IllegalArgumentException(show"Case ${command.caseId} does not exist"))
      .flatMap(state =>
        if (command.description == state.description)
          new IllegalArgumentException(s"Description is already: ${state.description}").asLeft
        else
          DescriptionChanged(
            caseId = command.caseId,
            description = command.description,
            userId = command.userId,
            timestamp = timestamp,
          ).asRight
      )
}

case class ChangeDescriptionHandler[F[_]: Temporal](
  messageDb: MessageDb[F],
) {
  import ChangeDescriptionHandler._

  def now: F[ZonedDateTime] = 
    Clock[F].realTime.map(d => Instant.ofEpochMilli(d.toMillis).atZone(ZoneOffset.UTC))

  def read(caseId: UUID): F[Option[Case]] = 
    messageDb
      .getAllStreamMessages(Event.streamName(caseId))
      .compile
      .fold(none[Case])(Case.update)

  def write(event: DescriptionChanged, version: Long): F[Unit] = 
    messageDb.writeMessage(
      id = UUID.randomUUID(),
      streamName = Event.streamName(event.caseId),
      `type` = event.getClass.getSimpleName,
      data = event.asJson,
      metadata = none,
      expectedVersion = version.some,
    ).void

  def handle(command: ChangeDescription): F[Unit] = 
    for {
      state <- read(command.caseId)
      ts <- now
      event <- changeDescription(command, state, ts).liftTo[F]
      () <- write(event, state.map(_.version).getOrElse(throw new IllegalStateException("TODO this is dumb")))
    } yield ()
}


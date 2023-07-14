package watching

import cats.syntax.all._
import cats.effect._
import java.util.UUID
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import messagedb._
import io.circe.syntax._

object WatchingHandler {

  case class State(
    watching: Boolean,
    version: Long,
  )

  object State {
    val initial = State(false, -1)

    def update(userId: UUID)(o: State, m: MessageDb.Read.Message): State = 
      m.`type` match {
        case "Watched" if m.dataAs[Watched].toOption.exists(_.userId === userId) =>
          o.copy(
            watching = true,
            version = m.position,
          )
        case "Unwatched" if m.dataAs[Unwatched].toOption.exists(_.userId === userId) =>
          o.copy(
            watching = false,
            version = m.position,
          )
        case _ =>
          o.copy(
            version = m.position
          )
      }

  }

  def watch(
    state: State,
    command: Watch,
    timestamp: ZonedDateTime,
  ): Either[Throwable, Watched] = 
    if (state.watching)
      new IllegalArgumentException(show"User ${command.userId} is already watching case ${command.caseId}").asLeft
    else
      Watched(
        caseId = command.caseId,
        userId = command.userId,
        timestamp = timestamp,
      ).asRight

  def unwatch(
    state: State,
    command: Unwatch,
    timestamp: ZonedDateTime,
  ): Either[Throwable, Unwatched] = 
    if (state.watching)
      Unwatched(
        caseId = command.caseId,
        userId = command.userId,
        timestamp = timestamp,
      ).asRight
    else
      new IllegalArgumentException(show"User ${command.userId} is not watching case ${command.caseId}").asLeft

}

case class WatchingHandler[F[_]: Temporal](
  messageDb: MessageDb[F],
) {
  import WatchingHandler._

  //TODO extract
  //TODO try refatoring ZonedDateTime to OffsetDateTime
  def now: F[ZonedDateTime] = 
    Clock[F].realTime.map(d => Instant.ofEpochMilli(d.toMillis).atZone(ZoneOffset.UTC))

  def read(caseId: UUID, userId: UUID): F[State] = 
    messageDb
      .getAllStreamMessages(Event.streamName(caseId))
      .compile
      .fold(State.initial)(State.update(userId))

  def write(event: Watched, version: Long): F[Unit] = 
    messageDb.writeMessage(
      id = UUID.randomUUID(),
      streamName = Event.streamName(event.caseId),
      `type` = event.getClass.getSimpleName,
      data = event.asJson,
      metadata = none,
      expectedVersion = version.some,
    ).void

  def write(event: Unwatched, version: Long): F[Unit] = 
    messageDb.writeMessage(
      id = UUID.randomUUID(),
      streamName = Event.streamName(event.caseId),
      `type` = event.getClass.getSimpleName,
      data = event.asJson,
      metadata = none,
      expectedVersion = version.some,
    ).void

  def watch(command: Watch): F[Unit] = 
    for {
      state <- read(command.caseId, command.userId)
      ts <- now
      event <- WatchingHandler.watch(state, command, ts).liftTo[F]
      () <- write(event, state.version)
    } yield ()

  def unwatch(command: Unwatch): F[Unit] = 
    for {
      state <- read(command.caseId, command.userId)
      ts <- now
      event <- WatchingHandler.unwatch(state, command, ts).liftTo[F]
      () <- write(event, state.version)
    } yield ()

}

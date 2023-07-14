//TODO does not compile

/*package notifications

import cats._
import cats.syntax.all._
import cats.effect._
import messagedb._
import java.util.UUID
import java.time.ZonedDateTime
import io.circe.syntax._

/*
at-least-once
- receive command
- perform side effect
  - assume this is not idempotent
  - if it is, then we get effectively/exactly once!
- record event
  - record event on success
  - what if side effect fails?
    - could record failure event
    - could report failure some other way (log, alert)
    - lack of recording success event allows for retries
- commit subscriber offset
  - essentially marks the command as "done"
  - after restart, won't re-read the command
  - may not commit offset for every message
    - every n seconds
    - every n messages
  - reprocessing command due to uncommitted offset:
    - recorded event prevents reperforming side effect
    - if no event, then we reperform the side effect
      - remember, this is at-least-once, when failure occurs

at-most-once
- swap the order:
  1. record the event
  2. perform the side effect
*/

object NotificationHandler {

  case class State()

  object State

  def sendEmail[F[_]](
    state: State,
    command: SendEmail,
    emailer: Emailer[F],
    from: String,
    timestamp: ZonedDateTime,
  ): F[Event] = ???
}

case class NotificationHandler[F[_]: Temporal](
  emailer: Emailer[F],
  from: String,
  messageDb: MessageDb[F],
) {

  def writeSuccess(event: SentEmail, version: Long): F[Unit] = 
    messageDb.writeMessage(
      id = UUID.randomUUID(),
      streamName = Event.streamName(event.notificationId),
      `type` = event.getClass.getSimpleName,
      data = event.asJson,
      metadata = none,
      expectedVersion = version.some,
    ).void

  def writeFailure(failure: Throwable): F[Unit] = ???
  
  def sendEmail(command: SendEmail): F[Unit] = 
    for {
      // state <- read()
      // () <- validate(state)
      e <- emailer.send(
        Emailer.Email(
          from = from,
          to = command.to,
          subject = command.subject,
          text = command.text,
        )
      ).attempt
      () <- e.fold(writeFailure(_), writeSuccess(_))
    } yield ()

  def handle(command: MessageDb.Read.Message): F[Unit] = 
    //TODO this pattern of matching on the type name, then dealing with a decode failure, needs to be improved
    command.`type` match {
      case "SendEmail" =>
        for {
          data <- command.dataAs[SendEmail].liftTo[F]
          () <- sendEmail(data)
        } yield ()
      case _ => 
        Applicative[F].unit
    }
}
*/
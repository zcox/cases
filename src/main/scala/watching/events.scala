package watching

import cats.syntax.all._
import java.util.UUID
import java.time.ZonedDateTime
import io.circe._
import io.circe.generic.semiauto._

sealed trait Event

object Event {
  val category = "cases:watching"
  def streamName(caseId: UUID): String = 
    show"$category-$caseId"
}

//TODO do we need to record how the watch change occurred? e.g. user clicked button, some automation, etc

sealed trait Cause

case class UserAction() extends Cause
case class Automation() extends Cause

case class Watched(
  caseId: UUID,
  userId: UUID,
  timestamp: ZonedDateTime,
) extends Event

object Watched {
  implicit val encoder: Encoder[Watched] = deriveEncoder
  implicit val decoder: Decoder[Watched] = deriveDecoder
}

case class Unwatched(
  caseId: UUID,
  userId: UUID,
  timestamp: ZonedDateTime,
) extends Event

object Unwatched {
  implicit val encoder: Encoder[Unwatched] = deriveEncoder
  implicit val decoder: Decoder[Unwatched] = deriveDecoder
}

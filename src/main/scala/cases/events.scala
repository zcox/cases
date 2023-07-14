package cases

import cats.syntax.all._
import java.util.UUID
import java.time.ZonedDateTime
import io.circe._
import io.circe.generic.semiauto._

sealed trait Event

object Event {
  def streamName(caseId: UUID): String = 
    show"cases-$caseId"
}

case class Started(
  tenantId: UUID,
  caseId: UUID,
  title: String,
  creatorId: UUID,
  timestamp: ZonedDateTime,
) extends Event

object Started {
  implicit val encoder: Encoder[Started] = deriveEncoder
  implicit val decoder: Decoder[Started] = deriveDecoder
}

case class DescriptionChanged(
  caseId: UUID,
  description: String,
  userId: UUID,
  timestamp: ZonedDateTime,
) extends Event

object DescriptionChanged {
  implicit val encoder = deriveEncoder[DescriptionChanged]
  implicit val decoder = deriveDecoder[DescriptionChanged]
}

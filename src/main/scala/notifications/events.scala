package notifications

import java.util.UUID
import io.circe.generic.semiauto._

sealed trait Event

object Event {
  val category = "notifications"
  def streamName(notificationId: UUID): String =
    s"$category-$notificationId"
}

case class SentEmail(
  notificationId: UUID,
  to: String,
  subject: String,
  text: String,
) extends Event

object SentEmail {
  implicit val encoder = deriveEncoder[SentEmail]
}

case class FailedEmail(
  notificationId: UUID,
  to: String,
  subject: String,
  text: String,
  reason: String,
) extends Event

object FailedEmail {
  implicit val encoder = deriveEncoder[FailedEmail]
}

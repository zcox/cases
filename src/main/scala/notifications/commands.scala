package notifications

import java.util.UUID
import io.circe.generic.semiauto._

sealed trait Command

object Command {
  val category = "notifications:commands"
  def streamName(notificationId: UUID): String =
    s"$category-$notificationId"
}

case class SendEmail(
  notificationId: UUID,
  to: String,
  subject: String,
  text: String,
) extends Command

object SendEmail {
  implicit val decoder = deriveDecoder[SendEmail]
}

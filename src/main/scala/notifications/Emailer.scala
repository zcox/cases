package notifications

import cats._
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import java.util.UUID

trait Emailer[F[_]] {
  import Emailer._

  def send(email: Email): F[Success]
}

object Emailer {

  case class Email(
    from: String,
    to: String,
    subject: String,
    text: String,
  )

  case class Success(
    receiptId: UUID,
  )

  def fake[F[_]: Functor: Logger](): Emailer[F] =
    new Emailer[F] {
      override def send(email: Email): F[Success] = {
        //TODO random failures
        val receiptId = UUID.randomUUID()
        Logger[F]
          .info(s"Sent email with receiptId = $receiptId: $email")
          .as(Success(receiptId))
      }
    }
}

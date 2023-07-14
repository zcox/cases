package main

import java.util.UUID
import cats.Applicative
import cats.data._
import org.http4s.Request

case class User(
  userId: UUID,
  tenantId: UUID,
)

object User {
  val dummy = User(
    UUID.nameUUIDFromBytes("dummyUserId".getBytes), 
    UUID.nameUUIDFromBytes("dummyTenantId".getBytes),
  )

  def dummyAuth[F[_]: Applicative]: Kleisli[OptionT[F, *], Request[F], User] =
    Kleisli(_ => OptionT.pure(dummy))
}

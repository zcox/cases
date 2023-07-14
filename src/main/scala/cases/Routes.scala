package cases

import cats.syntax.all._
import cats.effect._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import java.util.UUID
import main.User

object Routes {

  case class PutCaseRequest(
    title: String,
  ) {
    def toCommand(caseId: UUID, user: User): Start = 
      Start(
        tenantId = user.tenantId,
        caseId = caseId,
        title = this.title,
        creatorId = user.userId,
      )
  }

  object PutCaseRequest {
    implicit val decoder: Decoder[PutCaseRequest] = deriveDecoder
    implicit def entityDecoder[F[_]: Concurrent] = jsonOf[F, PutCaseRequest]
  }

  case class PutDescriptionRequest(
    description: String
  ) {
    def toCommand(caseId: UUID, user: User): ChangeDescription = 
      ChangeDescription(
        caseId = caseId,
        description = this.description,
        userId = user.userId,
      )
  }

  object PutDescriptionRequest {
    implicit val decoder = deriveDecoder[PutDescriptionRequest]
    implicit def entityDecoder[F[_]: Concurrent] = jsonOf[F, PutDescriptionRequest]
  }

  def routes[F[_]: Temporal](
    startHandler: StartHandler[F],
    changeDescriptionHandler: ChangeDescriptionHandler[F],
    read: Read[F],
  ): AuthedRoutes[User, F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    AuthedRoutes.of[User, F] {

      case ar @ PUT -> Root / "cases" / UUIDVar(caseId) as user =>
        for {
          body <- ar.req.as[PutCaseRequest]
          //TODO handle errors
          () <- startHandler.handle(body.toCommand(caseId, user))
          resp <- Ok()
        } yield resp

      case ar @ PUT -> Root / "cases" / UUIDVar(caseId) / "description" as user =>
        for {
          body <- ar.req.as[PutDescriptionRequest]
          //TODO handle errors
          () <- changeDescriptionHandler.handle(body.toCommand(caseId, user))
          resp <- Ok()
        } yield resp

      //TODO could extract query endpoints out into separate routes
      case GET -> Root / "cases" as user =>
        Ok(read.getCasesByTenantId(user.tenantId).map(_.asJson))

    }
  }
}

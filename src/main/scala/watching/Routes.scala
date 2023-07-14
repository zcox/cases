package watching

import cats.syntax.all._
import cats.effect._
import io.circe.syntax._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import main.User

object Routes {

  def routes[F[_]: Temporal](
    handler: WatchingHandler[F],
    view: Read[F],
  ): AuthedRoutes[User, F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    AuthedRoutes.of[User, F] {

      case POST -> Root / "cases" / UUIDVar(caseId) / "watch" as user => 
        for {
          () <- handler.watch(Watch(caseId, user.userId))
          resp <- Ok()
        } yield resp

      case POST -> Root / "cases" / UUIDVar(caseId) / "unwatch" as user => 
        for {
          () <- handler.unwatch(Unwatch(caseId, user.userId))
          resp <- Ok()
        } yield resp

      case GET -> Root / "cases" / UUIDVar(caseId) / "watching" as _ =>
        Ok(view.watchers(caseId).map(_.asJson))

    }
  }
}

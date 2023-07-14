package main

import cats.syntax.all._
import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import messagedb._
import org.http4s.server.AuthMiddleware

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      () <- Stream.eval(Flyway.migrate[IO]("localhost", 5433, "postgres", "postgres", "postgres"))

      messageDbSessionPool <- Stream.resource(Skunk.messageDbSessionPool[IO]())
      messageDb = MessageDb.fromPool2[IO](messageDbSessionPool)

      viewSessionPool <- Stream.resource(Skunk.viewSessionPool[IO]())
      casesViewRead = cases.Read.fromPool[IO](viewSessionPool)
      casesViewWrite = cases.Write.fromPool[IO](viewSessionPool)
      casesViewAggregator = cases.Aggregator[IO](messageDb, casesViewWrite)

      watchingViewRead = watching.Read.fromPool[IO](viewSessionPool)
      watchingViewWrite = watching.Write.fromPool[IO](viewSessionPool)
      watchingAggregator = watching.Aggregator[IO](messageDb, watchingViewWrite)

      authMiddleware = AuthMiddleware.withFallThrough(User.dummyAuth[IO])

      startHandler = cases.StartHandler[IO](messageDb)
      changeDescriptionHandler = cases.ChangeDescriptionHandler[IO](messageDb)
      casesRoutes = authMiddleware(cases.Routes.routes[IO](
        startHandler, 
        changeDescriptionHandler,
        casesViewRead,
      ))

      watchingHandler = watching.WatchingHandler[IO](messageDb)
      watchingRoutes = authMiddleware(watching.Routes.routes[IO](watchingHandler, watchingViewRead))

      x <- Server.stream[IO](casesRoutes <+> watchingRoutes)
            .concurrently(casesViewAggregator)
            .concurrently(watchingAggregator)
    } yield x).compile.drain.as(ExitCode.Success)
    
}

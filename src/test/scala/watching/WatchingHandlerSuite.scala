package watching

import cats.syntax.all._
import munit.ScalaCheckSuite
import org.scalacheck.Prop._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.magnolia._
import java.time.ZonedDateTime
import WatchingHandler._

class WatchingHandlerSuite extends ScalaCheckSuite {

  val watching = 
    for {
      s <- arbitrary[State]
      state = s.copy(watching = true)
    } yield state

  val notWatching = 
    for {
      s <- arbitrary[State]
      state = s.copy(false)
    } yield state

  property("Given watching, when watch, then error") {
    forAll(watching, arbitrary[Watch], arbitrary[ZonedDateTime]) { (state, command, ts) =>
      val result = watch(state, command, ts)
      assert(result.isLeft)
    }
  }

  property("Given not watching, when watch, then watched") {
    forAll(notWatching, arbitrary[Watch], arbitrary[ZonedDateTime]) { case (state, command, ts) =>
      val result = watch(state, command, ts)
      val expected = Watched(
        caseId = command.caseId,
        userId = command.userId,
        timestamp = ts,
      ).asRight[Throwable]
      assertEquals(result, expected)
    }
  }

  //TODO unwatch

}

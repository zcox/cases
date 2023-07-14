package cases

import cats.syntax.all._
import munit.ScalaCheckSuite
import org.scalacheck.Prop._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.magnolia._
import java.time.ZonedDateTime
import ChangeDescriptionHandler._

class ChangeDescriptionHandlerSuite extends ScalaCheckSuite {

  val validStateAndChange = 
    for {
      state <- arbitrary[Case]
      d <- arbitrary[String] suchThat (_ != state.description)
      c <- arbitrary[ChangeDescription]
      command = c.copy(description = d)
    } yield (state, command)

  property("Given no case, when change, then Error") {
    forAll { (command: ChangeDescription, ts: ZonedDateTime) =>
      val result = changeDescription(command, none, ts)
      assert(result.isLeft)
    }
  }

  property("Given case, when change to same description, then error") {
    forAll { (state: Case, c: ChangeDescription, ts: ZonedDateTime) =>
      val command = c.copy(description = state.description)
      val result = changeDescription(command, state.some, ts)
      assert(result.isLeft)
    }
  }

  property("Given case, when change description, then changed") {
    forAll(validStateAndChange, arbitrary[ZonedDateTime]) { case ((state, command), ts) =>
      val result = changeDescription(command, state.some, ts)
      val expected = DescriptionChanged(
        caseId = command.caseId,
        description = command.description,
        userId = command.userId,
        timestamp = ts,
      ).asRight[Throwable]
      assertEquals(result, expected)
    }
  }

}

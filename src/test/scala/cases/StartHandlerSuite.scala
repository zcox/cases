package cases

import cats.syntax.all._
import munit.ScalaCheckSuite
import org.scalacheck._
import org.scalacheck.Prop._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.magnolia._
import java.time.ZonedDateTime

class StartHandlerSuite extends ScalaCheckSuite {

  val nonEmptyString = 
    Gen.nonEmptyListOf[Char](Arbitrary.arbChar.arbitrary).map(_.mkString)

  val validStart = 
    for {
      s <- arbitrary[Start]
      t <- nonEmptyString
    } yield s.copy(title = t)

  property("When valid Start, Then Started") {
    forAll(validStart, arbitrary[ZonedDateTime]) { (command, ts) =>
      val result = StartHandler.start(command, ts)
      val expected = Started(
        tenantId = command.tenantId,
        caseId = command.caseId,
        title = command.title,
        creatorId = command.creatorId,
        timestamp = ts,
      ).asRight[Throwable]
      assertEquals(result, expected)
    }
  }

  property("When Start with empty title, Then Error") {
    forAll { (command: Start, ts: ZonedDateTime) =>
      val result = StartHandler.start(command.copy(title = ""), ts)
      assert(result.isLeft)
    }
  }

}

package watching

import java.util.UUID

sealed trait Command

case class Watch(
  caseId: UUID,
  userId: UUID,
) extends Command

case class Unwatch(
  caseId: UUID,
  userId: UUID,
) extends Command()

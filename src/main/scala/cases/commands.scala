package cases

import java.util.UUID

sealed trait Command

case class Start(
  tenantId: UUID,
  caseId: UUID,
  title: String,
  creatorId: UUID,
) extends Command

case class ChangeDescription(
  caseId: UUID,
  description: String,
  userId: UUID,
) extends Command

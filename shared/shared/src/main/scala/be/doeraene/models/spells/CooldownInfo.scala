package be.doeraene.models.spells

final case class CooldownInfo(startTime: Double, duration: Double, enabled: Boolean):
  def ready: Boolean = duration == 0

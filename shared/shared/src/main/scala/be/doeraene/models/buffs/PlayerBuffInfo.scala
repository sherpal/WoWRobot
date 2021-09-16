package be.doeraene.models.buffs

final case class PlayerBuffInfo(name: String, count: Int, debuffType: String, duration: Double, expirationTime: Double, source: String, spellId: Int)

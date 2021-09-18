package be.doeraene.models

import be.doeraene.models.buffs.PlayerBuffInfo
import be.doeraene.models.spells.CastSpellSucceeded
import io.circe.Decoder
import io.circe.generic.semiauto.*

final case class FullGameState(
    usedAbilities: Vector[CastSpellSucceeded],
    inCombat: Boolean,
    playerBuffs: Vector[PlayerBuffInfo]
                              )

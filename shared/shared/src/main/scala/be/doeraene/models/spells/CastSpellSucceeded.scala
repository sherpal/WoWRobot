package be.doeraene.models.spells

final case class CastSpellSucceeded(
    unitTarget: String,
    castGUID: String,
    timeInSeconds: Double,
    spellInfo: SpellInfo,
    cooldownInfo: CooldownInfo
)

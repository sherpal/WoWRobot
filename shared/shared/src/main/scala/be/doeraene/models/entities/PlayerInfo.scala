package be.doeraene.models.entities

import be.doeraene.models.PowerType

final case class PlayerInfo(
    name: String,
    currentLife: Int,
    maxLife: Int,
    powerType: String,
    powerValue: Int,
    powerValueMax: Int
)

package be.doeraene.data

import be.doeraene.models.GameState

object GameStateDistributor extends DistributorActor {
  type Element = GameState
}

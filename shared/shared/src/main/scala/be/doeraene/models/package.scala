package be.doeraene

import io.circe.Json

package object models {
  type GameState = Json

  type Primitive = Int | Double | Boolean | String
}

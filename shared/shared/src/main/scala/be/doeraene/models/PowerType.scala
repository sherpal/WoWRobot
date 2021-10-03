package be.doeraene.models

sealed trait PowerType {
  def stringValue: String
}

object PowerType {

  case object Mana extends PowerType {
    def stringValue: String = "MANA"
  }

  case object Rage extends PowerType {
    def stringValue: String = "RAGE"
  }

  case object Energy extends PowerType {
    def stringValue: String = "ENERGY"
  }

  case object Focus extends PowerType {
    def stringValue: String = "FOCUS"
  }

  case object ComboPoints extends PowerType {
    def stringValue: String = "COMBO_POINTS"
  }

  /** "COMBO_POINTS" "RUNES" "RUNIC_POWER" "SOUL_SHARDS" "ECLIPSE" "HOLY_POWER" "AMMOSLOT" (vehicles, 3.1) "FUEL"
    * (vehicles, 3.1)
    */

  case object Unknown extends PowerType {
    def stringValue: String = "UNKNOWN"
  }

  def powerTypes = List(Mana, Rage, Energy, Focus, ComboPoints)

  def fromStringValue(value: String): Option[PowerType] = powerTypes.find(_.stringValue == value)

  def fromStringValueWithUnknown(value: String): PowerType =
    fromStringValue(value).getOrElse(Unknown)

}

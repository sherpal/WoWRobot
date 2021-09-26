package be.doeraene.models

import be.doeraene.models.buffs.PlayerBuffInfo
import be.doeraene.models.entities.{PlayerInfo, TotemInfo}
import be.doeraene.models.spells.CastSpellSucceeded
import io.circe.Decoder
import io.circe.generic.semiauto.*

final case class FullGameState(
    usedAbilities: Vector[CastSpellSucceeded],
    inCombat: Boolean,
    playerBuffs: Vector[PlayerBuffInfo],
    playersInfo: Vector[PlayerInfo],
    maybeTotemsInfo: Option[Vector[TotemInfo]]
):
  def playerIndexByName: Map[String, Int] = playersInfo.map(_.name).zipWithIndex.toMap

  lazy val presentTotems: Vector[TotemInfo.AliveTotemInfo] = maybeTotemsInfo.toVector.flatten.collect {
    case info: TotemInfo.AliveTotemInfo => info
  }

  def hasTotemWithIndex(index: Int): Boolean = presentTotems.exists(_.index == index)

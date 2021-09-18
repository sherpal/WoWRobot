package be.doeraene.models

import be.doeraene.data.communication.arrayreader.ArrayReader
import be.doeraene.models.buffs.PlayerBuffInfo
import be.doeraene.models.spells.CastSpellSucceeded
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object FullGameStateDTO {
  private implicit val anyDecoder: Decoder[Primitive] = List[Decoder[Primitive]](
    Decoder[Int].map(identity[Primitive]),
    Decoder[Double].map(identity[Primitive]),
    Decoder[Boolean].map(identity[Primitive]),
    Decoder[String].map(identity[Primitive])
  ).reduce(_.or(_))

  private case class GameStateHere(
      usedAbilities: Vector[Vector[Primitive]],
      inCombat: Boolean,
      playerBuffs: Vector[Vector[Primitive]]
  ) {
    def translateVectorOfInfos[T](info: Vector[Vector[Primitive]])(using reader: ArrayReader[Vector[T]]) =
      reader.extractInfoIgnoreCount(info.length +: info.flatten, 0)

    def toFullGameState = for {
      decodedUsedAbilities <- translateVectorOfInfos[CastSpellSucceeded](usedAbilities)
      decodedPlayerBuffs <- translateVectorOfInfos[PlayerBuffInfo](playerBuffs)
    } yield FullGameState(decodedUsedAbilities, inCombat, decodedPlayerBuffs)
  }

  private val gameStateHereDecoder: Decoder[GameStateHere] = deriveDecoder

  val fullGameStateDecoder: Decoder[FullGameState] = gameStateHereDecoder.emapTry(_.toFullGameState.toTry)

}

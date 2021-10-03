package be.doeraene.models

import be.doeraene.data.communication.arrayreader.{ArrayReader, AutoArrayReader}
import be.doeraene.models.buffs.PlayerBuffInfo
import be.doeraene.models.entities.{PlayerInfo, TotemInfo}
import be.doeraene.models.spells.CastSpellSucceeded
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import be.doeraene.models.PowerType

object FullGameStateDTO {
  private implicit val anyDecoder: Decoder[Primitive] = List[Decoder[Primitive]](
    Decoder[Int].map(identity[Primitive]),
    Decoder[Double].map(identity[Primitive]),
    Decoder[Boolean].map(identity[Primitive]),
    Decoder[String].map(identity[Primitive])
  ).reduce(_.or(_))

  private case class RawTotem(index: Int, haveTotem: Boolean, name: String, startTime: Int, duration: Int):
    def toTotemInfo: TotemInfo =
      if haveTotem then TotemInfo.AliveTotemInfo(index, name, startTime, duration)
      else TotemInfo.AbsentTotemInfo(index)

  private case class GameStateHere(
      usedAbilities: Vector[Vector[Primitive]],
      inCombat: Boolean,
      playerBuffs: Vector[Vector[Primitive]],
      playersInfo: Vector[Vector[Primitive]],
      totems: Option[Vector[Vector[Primitive]]]
  ) {
    def translateVectorOfInfos[T](info: Vector[Vector[Primitive]])(using reader: ArrayReader[Vector[T]]) =
      reader.extractInfoIgnoreCount(info.length +: info.flatten, 0)

    def toFullGameState = for {
      decodedUsedAbilities <- translateVectorOfInfos[CastSpellSucceeded](usedAbilities)
      decodedPlayerBuffs <- translateVectorOfInfos[PlayerBuffInfo](playerBuffs)
      decodedPlayersInfo <- translateVectorOfInfos[PlayerInfo](playersInfo)
      decodedTotemInfo <- totems.fold(Right(Option.empty[Vector[TotemInfo]]))(
        translateVectorOfInfos[RawTotem](_).map(_.map(_.toTotemInfo)).map(Some(_))
      )
    } yield FullGameState(decodedUsedAbilities, inCombat, decodedPlayerBuffs, decodedPlayersInfo, decodedTotemInfo)
  }

  private val gameStateHereDecoder: Decoder[GameStateHere] = deriveDecoder

  val fullGameStateDecoder: Decoder[FullGameState] = gameStateHereDecoder.emapTry(_.toFullGameState.toTry)

}

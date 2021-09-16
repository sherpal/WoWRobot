package be.doeraene.ias.paladin.ret

import akka.stream.scaladsl.Source
import be.doeraene.data.communication.arrayreader.ArrayReader
import be.doeraene.models.GameState
import be.doeraene.models.buffs.PlayerBuffInfo
import be.doeraene.models.spells.CastSpellSucceeded
import be.doeraene.services.robot.*
import be.doeraene.services.robot.Keyboard.*
import zio.{UIO, ZIO}
import io.circe.Decoder
import io.circe.generic.auto.*
import oshi.SystemInfo
import zio.duration.*

import java.time.ZoneOffset

package object lowlevel {

  val castSealOfRighteousness = pressAndReleaseKey(Q)
  val castJudgement = pressAndReleaseKey(E)

  val judgementAndSealOfRighteaouness = castJudgement *> castSealOfRighteousness.delay(10.millis)

  type LowLevelRetGameState = GameState

  def fromGameState(gameState: GameState): LowLevelRetGameState = gameState

  private type Primitive = Int | Double | Boolean | String
  private case class GameStateHere(
      usedAbilities: Vector[Vector[Primitive]],
      inCombat: Boolean,
      playerBuffs: Vector[Vector[Primitive]]
  )

  private implicit val anyDecoder: Decoder[Primitive] = List[Decoder[Primitive]](
    Decoder[Int].map(identity[Primitive]),
    Decoder[Double].map(identity[Primitive]),
    Decoder[Boolean].map(identity[Primitive]),
    Decoder[String].map(identity[Primitive])
  ).reduce(_.or(_))

  def takeAction(gameState: LowLevelRetGameState) = for {
    gsHere <- ZIO.fromEither(Decoder[GameStateHere].decodeJson(gameState))
    spellsInfo <- ZIO.fromEither(
      ArrayReader[Vector[CastSpellSucceeded]]
        .extractInfoIgnoreCount(gsHere.usedAbilities.length +: gsHere.usedAbilities.flatten, 0)
    )
    buffsInfo <- ZIO.fromEither(
      ArrayReader[Vector[PlayerBuffInfo]]
        .extractInfoIgnoreCount(gsHere.playerBuffs.length +: gsHere.playerBuffs.flatten, 0)
    )
    isSealOfRighteousnessActive <- UIO(buffsInfo.exists(_.spellId == 21084))
    judgementCanBeCast = !spellsInfo.filter(_.spellInfo.spellId == 20271).exists(!_.cooldownInfo.ready)
    _ <- ZIO.ifM(UIO(isSealOfRighteousnessActive))(
      ZIO.when(gsHere.inCombat && judgementCanBeCast)(judgementAndSealOfRighteaouness),
      castSealOfRighteousness
    )
  } yield ()

}

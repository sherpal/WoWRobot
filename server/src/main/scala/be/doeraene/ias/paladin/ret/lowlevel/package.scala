package be.doeraene.ias.paladin.ret

import akka.stream.scaladsl.Source
import be.doeraene.data.communication.arrayreader.ArrayReader
import be.doeraene.models.{FullGameState, GameState}
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

  def takeAction(gameState: FullGameState) = for {
    spellsInfo <- UIO(gameState.usedAbilities)
    buffsInfo <- UIO(gameState.playerBuffs)
    isSealOfRighteousnessActive <- UIO(buffsInfo.exists(_.spellId == 21084))
    judgementCanBeCast = !spellsInfo.filter(_.spellInfo.spellId == 20271).exists(!_.cooldownInfo.ready)
    _ <- ZIO.ifM(UIO(isSealOfRighteousnessActive))(
      ZIO.when(gameState.inCombat && judgementCanBeCast)(judgementAndSealOfRighteaouness),
      castSealOfRighteousness
    )
  } yield ()

}

package be.doeraene.ias.shaman.heal

import be.doeraene.models.FullGameState
import be.doeraene.services.robot.*
import be.doeraene.services.robot.Keyboard.*
import be.doeraene.ias.targetGroupMember
import be.doeraene.models.entities.TotemInfo
import com.typesafe.config.ConfigFactory
import zio.{UIO, ZIO}

package object lowlevel {

  lazy val config = ConfigFactory.load()

  val castLesserHealingWave = pressAndReleaseKey(O)
  val castHealingWave = pressAndReleaseKey(P)
  val castHealingTotem = pressAndReleaseKey(I)

  val drink = pressAndReleaseKey(U)

  lazy val maxLesserHealingWaveAmount = config.getInt("WoW.shaman.low-level.maxLesserHealingWaveAmount")
  lazy val maxHealingWaveAmount = config.getInt("WoW.shaman.low-level.maxHealingWaveAmount")
  lazy val healingTotemAmount = config.getInt("WoW.shaman.low-level.healingTotemAmount")

  def inCombatTakeAction(gameState: FullGameState) = for {
    spellsInfo <- UIO(gameState.usedAbilities)
    buffsInfo <- UIO(gameState.playerBuffs)
    playersInfo <- UIO(gameState.playersInfo)
    totemsInfo <- UIO(gameState.maybeTotemsInfo.getOrElse(TotemInfo.empty))

    shouldCastHealingTotem <- UIO(
      !gameState.hasTotemWithIndex(3) && playersInfo.count(info =>
        info.maxLife - info.currentLife > healingTotemAmount
      ) >= 3
    )

    _ <- ZIO.when(shouldCastHealingTotem)(castHealingTotem)

    maybePlayerToHeal <- UIO(playersInfo.find(info => info.maxLife - info.currentLife > maxLesserHealingWaveAmount))
    _ <- maybePlayerToHeal
      .filter(_ => !shouldCastHealingTotem)
      .map(_.name)
      .flatMap(gameState.playerIndexByName.get) match {
      case Some(playerIndex) => targetGroupMember(playerIndex) *> castLesserHealingWave
      case None              => ZIO.effectTotal(println("No player to heal"))
    }
  } yield ()

  def outOfCombatTakeAction(gameState: FullGameState) = for {
    _ <- ZIO.unit
    shouldDrink <- UIO(
      !gameState.isDrinking &&
        gameState.playersInfo.headOption.exists(me => me.powerValue / me.powerValueMax.toDouble < 0.2)
    )
    _ <- ZIO.when(shouldDrink)(drink)
  } yield ()

  def takeAction(gameState: FullGameState) =
    ZIO.ifM(UIO(gameState.inCombat))(inCombatTakeAction(gameState), outOfCombatTakeAction(gameState))

}

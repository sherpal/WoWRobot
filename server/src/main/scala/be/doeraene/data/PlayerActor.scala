package be.doeraene.data

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import be.doeraene.models.GameState

object PlayerActor {

  sealed trait Command

  case class Update(gameState: GameState) extends Command

  def apply(): Behavior[Command] = waitingForFirstGameState

  private def waitingForFirstGameState: Behavior[Command] = Behaviors.receive { (context, command) =>
    command match {
      case Update(gameState) => behavior(gameState)
    }
  }

  private def behavior(currentGameState: GameState): Behavior[Command] = Behaviors.ignore

}

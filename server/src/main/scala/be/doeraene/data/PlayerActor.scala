package be.doeraene.data

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import be.doeraene.models.GameState
import be.doeraene.ias.paladin.ret.lowlevel._

import scala.concurrent.duration._

object PlayerActor {

  sealed trait Command

  case class Update(gameState: GameState) extends Command
  case object GameLoop extends Command

  def apply(): Behavior[Command] = waitingForFirstGameState

  private def waitingForFirstGameState: Behavior[Command] = Behaviors.setup[Command] { context =>
    context.scheduleOnce(500.millis, context.self, GameLoop)

    Behaviors.receiveMessage { command =>
      command match {
        case Update(gameState) => behavior(gameState)
        case GameLoop =>
          context.scheduleOnce(500.millis, context.self, GameLoop)
          Behaviors.same
        //case _ => Behaviors.ignore
      }
    }
  }

  private def behavior(currentGameState: GameState): Behavior[Command] = Behaviors.receive { (context, command) =>
    command match {
      case Update(gameState) => behavior(gameState)
      case GameLoop =>
        context.scheduleOnce(1000.millis, context.self, GameLoop)
        try {
          be.doeraene.ZIOConfig.theRuntime.unsafeRun(
            takeAction(fromGameState(currentGameState))
          )
        } catch {
          case t: Throwable => t.printStackTrace()
        }
        Behaviors.same
    }
  }

}

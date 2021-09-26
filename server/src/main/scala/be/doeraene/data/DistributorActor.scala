package be.doeraene.data

import akka.{Done, NotUsed}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{Flow, Keep, Sink}

import scala.concurrent.Future

trait DistributorActor {

  type Element

  sealed trait Command

  case class Update(element: Element) extends Command
  case class Subscribe[T](fromElement: Element => T, replyTo: ActorRef[T]) extends Command {
    def send(element: Element): Unit = replyTo ! fromElement(element)
  }
  case class Unsubscribe(ref: ActorRef[Nothing]) extends Command
  case class Terminate(maybeThrowable: Option[Throwable]) extends Command

  def apply(): Behavior[Command] = behaviorWaitingForFirstElement(List.empty)

  private def behaviorWaitingForFirstElement(
      subscriptions: List[Subscribe[_]]
  ): Behavior[Command] =
    Behaviors.receive { (context, command) =>
      command match {
        case update @ Update(element) =>
          context.self ! update
          behavior(subscriptions, element)
        case subscription: Subscribe[_] =>
          context.watchWith(subscription.replyTo, Unsubscribe(subscription.replyTo))
          behaviorWaitingForFirstElement(subscription +: subscriptions)
        case Unsubscribe(ref) =>
          behaviorWaitingForFirstElement(subscriptions.filterNot(_.replyTo == ref))
        case Terminate(maybeThrowable) =>
          maybeThrowable.foreach(context.log.error("Closing because of upstream error", _))
          Behaviors.stopped
      }
    }

  private def behavior(
      subscriptions: List[Subscribe[_]],
      currentElement: Element
  ): Behavior[Command] =
    Behaviors.receive { (context, command) =>
      command match {
        case Update(element) =>
          subscriptions.foreach(_.send(element))
          behavior(subscriptions, element)
        case subscription: Subscribe[_] =>
          context.watchWith(subscription.replyTo, Unsubscribe(subscription.replyTo))
          subscription.send(currentElement)
          behavior(subscription +: subscriptions, currentElement)
        case Unsubscribe(ref) =>
          behavior(subscriptions.filterNot(_.replyTo == ref), currentElement)
        case Terminate(maybeThrowable) =>
          maybeThrowable.foreach(context.log.error("Closing because of upstream error", _))
          Behaviors.stopped
      }
    }

  def sink(ref: ActorRef[Command]): Sink[Element, Future[Done]] =
    Flow[Element].map(Update(_)).toMat(Sink.foreach(ref ! _))(Keep.right)

}

object DistributorActor {
  type Aux[T] = DistributorActor {
    type Element = T
  }
}

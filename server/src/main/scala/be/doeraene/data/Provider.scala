package be.doeraene.data

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.slf4j.Logger

import scala.concurrent.duration.FiniteDuration

trait Provider[T]:
  val logger: Logger

  def provide(): T

  def source(every: FiniteDuration): Source[T, NotUsed] = Provider.source(every, this)

  def sourceLogErrorAndContinue[U](every: FiniteDuration)(using ev: T <:< Either[Throwable, U]): Source[U, NotUsed] =
    source(every)
      .map(ev)
      .alsoTo(
        Flow[Either[Throwable, U]]
          .collect { case Left(error) => error }
          .to(Sink.foreach(logger.error("Failed to decode game state", _)))
      )
      .collect { case Right(u) => u }

object Provider:
  def source[T](every: FiniteDuration, provider: Provider[T]): Source[T, NotUsed] =
    Source.repeat(()).throttle(1, every).map(_ => provider.provide())
end Provider

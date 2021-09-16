package be.doeraene.services.logging

import zio.{URIO, ZIO}

final class HelperLogger private[logging] () {

  /** Logs the message at DEBUG level. */
  def debug(message: String): URIO[Logging, Unit] = ZIO.accessM[Logging](_.get.debug(message))

  /** Logs the message at INFO level. */
  def info(message: String): URIO[Logging, Unit] = ZIO.accessM[Logging](_.get.info(message))

  /** Logs the message at WARN level. */
  def warn(message: String): URIO[Logging, Unit] = ZIO.accessM[Logging](_.get.warn(message))

  /** Logs the message at ERROR level. */
  def error(message: String): URIO[Logging, Unit] = ZIO.accessM[Logging](_.get.error(message))

  /** Logs the message at ERROR level, with the stack trace of the given [[Throwable]]. */
  def error(message: String, throwable: Throwable): URIO[Logging, Unit] =
    ZIO.accessM[Logging](_.get.error(message, throwable))

}

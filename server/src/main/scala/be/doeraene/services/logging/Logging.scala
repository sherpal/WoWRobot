package be.doeraene.services.logging

import org.slf4j.{Logger, LoggerFactory}
import zio._

object Logging {

  trait Service {
    def debug(message: => String): UIO[Unit]

    def info(message: => String): UIO[Unit]

    def warn(message: => String): UIO[Unit]

    def error(message: => String): UIO[Unit]

    def error(message: => String, throwable: => Throwable): UIO[Unit]

    def ++(that: Service): Service =
      factory(
        str => this.debug(str) *> that.debug(str),
        str => this.info(str) *> that.info(str),
        str => this.warn(str) *> that.warn(str),
        str => this.error(str) *> that.error(str),
        (str, throwable) => this.error(str, throwable) *> that.error(str, throwable)
      )
  }

  object Service {

    /** Returns a  [[Logging.Service]] delegating to a slf4 [[Logger]] */
    def fromLogger(logger: Logger): Service =
      effectFactory(
        logger.debug,
        logger.info,
        logger.warn,
        logger.error,
        logger.error
      )

    /** Returns a [[Service]] writing to the console (see `logback.xml` for more info). */
    def consoleLogger: Service = fromLogger(LoggerFactory.getLogger("console"))

    /** Returns a [[Logging.Service]] logging to the console and delegating to the specified [[Logger]]. */
    def fromLoggerWithConsole(logger: Logger): Service = fromLogger(logger) ++ consoleLogger

  }

  def fromLogger(logger: Logger): ULayer[Has[Service]] = ZLayer.succeed(Service.fromLogger(logger))

  def fromLoggerWithConsole(logger: Logger): ULayer[Has[Service]] =
    ZLayer.succeed(Service.fromLoggerWithConsole(logger))

  def consoleLogger: ULayer[Has[Service]] = ZLayer.succeed(Service.consoleLogger)

  private[logging] def effectFactory(
      debug0: String => Unit,
      info0: String => Unit,
      warn0: String => Unit,
      error0: String => Unit,
      error1: (String, Throwable) => Unit
  ): Service =
    factory(
      message => ZIO.effectTotal(debug0(message)),
      message => ZIO.effectTotal(info0(message)),
      message => ZIO.effectTotal(warn0(message)),
      message => ZIO.effectTotal(error0(message)),
      (message, throwable) => ZIO.effectTotal(error1(message, throwable))
    )

  private[logging] def factory(
      debug0: String => UIO[Unit],
      info0: String => UIO[Unit],
      warn0: String => UIO[Unit],
      error0: String => UIO[Unit],
      error1: (String, Throwable) => UIO[Unit]
  ): Service =
    new Service {
      def debug(message: => String): UIO[Unit] = debug0(message)

      def info(message: => String): UIO[Unit] = info0(message)

      def warn(message: => String): UIO[Unit] = warn0(message)

      def error(message: => String): UIO[Unit] = error0(message)

      def error(message: => String, throwable: => Throwable): UIO[Unit] = error1(message, throwable)
    }

}

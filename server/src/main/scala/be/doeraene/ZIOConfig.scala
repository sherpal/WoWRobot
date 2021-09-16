package be.doeraene

import be.doeraene.services
import be.doeraene.services.logging.Logging
import be.doeraene.services.robot.Robot
import org.slf4j.LoggerFactory
import zio.ZIO
import zio.console.Console
import zio.clock.Clock
import zio.blocking.Blocking

object ZIOConfig {
  type GlobalEnv = Clock with Blocking with Console with Logging with Robot

  val theRuntime: zio.Runtime[GlobalEnv] =
    zio.Runtime.default.unsafeRun(
      ZIO
        .runtime[GlobalEnv]
        .provideLayer(
          zio.clock.Clock.live ++ zio.blocking.Blocking.live ++ zio.console.Console.live ++ Logging
            .fromLoggerWithConsole(
              LoggerFactory.getLogger("be.doeraene")
            ) ++ services.robot.Robot.live
        )
    )

}

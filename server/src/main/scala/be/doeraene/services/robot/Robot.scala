package be.doeraene.services.robot

import be.doeraene.services.robot.Buttons.{Button, ButtonMask}
import be.doeraene.services.robot.Keyboard.KeyCode
import zio.{Has, ULayer, ZIO, ZLayer}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._

object Robot {

  trait Service {

    /** Waits for the specified [[Duration]]. */
    def delay(duration: Duration): ZIO[Clock, Nothing, Unit]

    /**
      * Moves the mouse to the specified coordinates.
      * (0, 0) is the top left pixel of the screen, x and y grow to the right and to the bottom, respectively.
      */
    def mouseMove(x: Int, y: Int): ZIO[Blocking, Nothing, Unit]

    /** Presses the specified [[ButtonMask]]. */
    def mousePress(button: ButtonMask): ZIO[Blocking, Nothing, Unit]

    /** Releases the specified [[ButtonMask]]. */
    def mouseRelease(button: ButtonMask): ZIO[Blocking, Nothing, Unit]

    /** Press the specified [[KeyCode]] */
    def keyPress(key: KeyCode): ZIO[Blocking, IllegalArgumentException, Unit]

    /** Release the specified [[KeyCode]] */
    def keyRelease(key: KeyCode): ZIO[Blocking, Nothing, Unit]

    /** Puts the given [[String]] in the clipboard, roughly equivalent to ctrl+c on Windows. */
    def copyToClipboard(str: String): ZIO[Blocking, RuntimeException, Unit]

  }

  object Service {

    def live: Service = new AwtRobot

  }

  def live: ULayer[Has[Service]] = ZLayer.succeed(Service.live)

}

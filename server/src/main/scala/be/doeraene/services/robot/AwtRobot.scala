package be.doeraene.services.robot

import be.doeraene.services.robot.Buttons.{Button, ButtonMask}
import be.doeraene.services.robot.Keyboard.KeyCode
import zio.ZIO
import zio.blocking.{blocking, Blocking}
import zio.clock.Clock
import zio.duration._

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

final class AwtRobot protected[robot]() extends Robot.Service {

  val robot = new java.awt.Robot

  private def delegate[A](effect: => A): ZIO[Blocking, Nothing, A] = blocking(ZIO.effectTotal(effect))

  def delay(duration: Duration): ZIO[Clock, Nothing, Unit] = zio.clock.sleep(duration)

  def mouseMove(x: Int, y: Int): ZIO[Blocking, Nothing, Unit] =
    delegate(robot.mouseMove(x, y))

  def mousePress(button: ButtonMask): ZIO[Blocking, Nothing, Unit] = delegate(robot.mousePress(button.value))

  def mouseRelease(button: ButtonMask): ZIO[Blocking, Nothing, Unit] = delegate(robot.mouseRelease(button.value))

  def keyPress(key: KeyCode): ZIO[Blocking, IllegalArgumentException, Unit] =
    ZIO.effect(robot.keyPress(key)).refineOrDie {
      case _: IllegalArgumentException => new IllegalArgumentException(s"The keycode $key was apparently not valid.")
    }

  def keyRelease(key: KeyCode): ZIO[Blocking, Nothing, Unit] =
    delegate(robot.keyRelease(key))

  def copyToClipboard(str: String): ZIO[Blocking, RuntimeException, Unit] =
    ZIO
      .effect {
        val stringSelection = new StringSelection(str)
        val clipboard       = Toolkit.getDefaultToolkit.getSystemClipboard
        clipboard.setContents(stringSelection, null)
      }
      .refineOrDie {
        case e: RuntimeException => e
      }

}

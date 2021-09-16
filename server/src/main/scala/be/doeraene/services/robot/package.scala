package be.doeraene.services

import be.doeraene.services.robot.Buttons.ButtonMask
import be.doeraene.services.robot.Keyboard.{
  alt,
  ctrl,
  digits,
  numpadDigits,
  KeyCode,
  ModifiedKey,
  ModifierKey,
  V
}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.{durationInt, Duration}
import zio.{Has, UIO, ZIO}

import scala.util.Try

package object robot {

  type Robot = Has[Robot.Service]

  /** Waits for the specified [[Duration]]. */
  def delay(duration: Duration): ZIO[Robot with Clock, Nothing, Unit] =
    ZIO.accessM(_.get[Robot.Service].delay(duration))

  /**
    * Moves the mouse to the specified coordinates.
    * (0, 0) is the top left pixel of the screen, x and y grow to the right and to the bottom, respectively.
    */
  def mouseMove(x: Int, y: Int): ZIO[Robot with Blocking, Nothing, Unit] =
    ZIO.accessM(_.get[Robot.Service].mouseMove(x, y))

  /** Presses the specified [[ButtonMask]]. */
  def mousePress(button: ButtonMask): ZIO[Robot with Blocking, Nothing, Unit] =
    ZIO.accessM(_.get.mousePress(button))

  /** Releases the specified [[ButtonMask]]. */
  def mouseRelease(button: ButtonMask): ZIO[Robot with Blocking, Nothing, Unit] =
    ZIO.accessM(_.get.mouseRelease(button))

  /** Presses and immediately release the specified [[ButtonMask]]. */
  def click(button: ButtonMask): ZIO[Robot with Blocking, Nothing, Unit] = mousePress(button) *> mouseRelease(button)

  /** Press the specified [[KeyCode]] */
  def keyPress(key: KeyCode): ZIO[Robot with Blocking, IllegalArgumentException, Unit] =
    ZIO.accessM(_.get.keyPress(key))

  /** Release the specified [[KeyCode]] */
  def keyRelease(key: KeyCode): ZIO[Robot with Blocking, Nothing, Unit] = ZIO.accessM(_.get.keyRelease(key))

  def pressAndReleaseKey(key: KeyCode): ZIO[Robot with Blocking, IllegalArgumentException, Unit] =
    keyPress(key) *> keyRelease(key)

  def pressAndReleaseWithModifier(
      modifierKey: ModifierKey,
      key: KeyCode
  ): ZIO[Robot with Blocking, IllegalArgumentException, Unit] =
    for {
      _ <- keyPress(modifierKey.code)
      _ <- keyPress(key)
      _ <- keyRelease(key)
      _ <- keyRelease(modifierKey.code)
    } yield ()

  def pressAltCode(code: List[Int]): ZIO[Robot with Blocking, IllegalArgumentException, Unit] =
    for {
      _ <- keyPress(alt.code)
      _ <- ZIO.foreach_(code) { digit =>
        pressAndReleaseKey(numpadDigits(digit))
      }
      _ <- keyPress(alt.code)
    } yield ()

  def pressAndReleaseWithModifier(
      modifiedKey: ModifiedKey
  ): ZIO[Robot with Clock with Blocking, IllegalArgumentException, Unit] =
    for {
      allKeys <- UIO(modifiedKey.code :: modifiedKey.modifierKey.map(_.code))
      _       <- ZIO.foreach_(allKeys.reverse)(keyPress(_) *> robot.delay(10.millis))
      _       <- ZIO.foreach_(allKeys)(keyRelease(_) *> robot.delay(10.millis))
    } yield ()

  def pressAndReleaseChar(char: Char): ZIO[Robot with Clock with Blocking, IllegalArgumentException, Unit] = {

    val maybeModifiedKey =
      (char, Keyboard.fromLetter(char), Try(char.toString.toInt).toOption.flatMap(Keyboard.fromDigit)) match {
        case (_, Some(code), _) => Some(ModifiedKey(code, Option.when(char.isUpper)(Keyboard.shift).toList))
        case (_, _, Some(code)) => Some(ModifiedKey(code, Nil))
        case (' ', _, _)        => Some(ModifiedKey(Keyboard.space, Nil))
        case ('\\', _, _)       => Some(ModifiedKey(Keyboard.backslash, Nil))
        case ('-', _, _)        => Some(ModifiedKey(Keyboard.dash, Nil))
        case (':', _, _)        => Some(ModifiedKey(Keyboard.semiColon, Keyboard.shift))
        case ('_', _, _)        => Some(Keyboard.shift + Keyboard.dash)
        case ('.', _, _)        => Some(ModifiedKey(Keyboard.dot, Nil))
        case _                  => None
      }

    for {
      modifiedKey <-
        ZIO.fromOption(maybeModifiedKey).orElseFail(new IllegalArgumentException(s"Char `$char` is not supported."))
      _ <- pressAndReleaseWithModifier(modifiedKey)
    } yield ()
  }

  def writeString(str: String): ZIO[Robot with Clock with Blocking, IllegalArgumentException, Unit] =
    ZIO.foreach_(str)(pressAndReleaseChar(_) *> delay(100.millis))

  def copyToClipboard(str: String): ZIO[Robot with Blocking, RuntimeException, Unit] =
    ZIO.accessM(_.get.copyToClipboard(str))

  /** Pastes from the clipboard, equivalent to ctrl+v on Windows. */
  def pasteFromClipboard: ZIO[Robot with Clock with Blocking, IllegalArgumentException, Unit] =
    robot.pressAndReleaseWithModifier(ctrl + V)

  def copyAndPaste(str: String): ZIO[Robot with Clock with Blocking, RuntimeException, Unit] =
    copyToClipboard(str) *> pasteFromClipboard

}

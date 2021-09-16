package be.doeraene.services.robot

import java.awt.event.{InputEvent, MouseEvent}

object Buttons {
  type Button = Int

  final case class ButtonMask(value: Int) extends AnyVal

  val button1: Button         = MouseEvent.BUTTON1
  val button1Mask: ButtonMask = ButtonMask(InputEvent.BUTTON1_DOWN_MASK)

}

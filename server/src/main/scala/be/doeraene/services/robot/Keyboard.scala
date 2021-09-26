package be.doeraene.services.robot

import java.awt.event.KeyEvent

object Keyboard {

  // todo[scala3] replace with opaque type
  type KeyCode = Int

  final case class ModifierKey(code: KeyCode) {
    def +(key: KeyCode): ModifiedKey = ModifiedKey(key, this)
  }

  final case class ModifiedKey(code: KeyCode, modifierKey: List[ModifierKey])
  object ModifiedKey {
    def apply(code: KeyCode, modifier: ModifierKey, others: ModifierKey*): ModifiedKey =
      ModifiedKey(code, modifier :: others.toList)
  }

  val A: KeyCode = KeyEvent.VK_A
  val B: KeyCode = KeyEvent.VK_B
  val C: KeyCode = KeyEvent.VK_C
  val D: KeyCode = KeyEvent.VK_D
  val E: KeyCode = KeyEvent.VK_E
  val F: KeyCode = KeyEvent.VK_F
  val G: KeyCode = KeyEvent.VK_G
  val H: KeyCode = KeyEvent.VK_H
  val I: KeyCode = KeyEvent.VK_I
  val J: KeyCode = KeyEvent.VK_J
  val K: KeyCode = KeyEvent.VK_K
  val L: KeyCode = KeyEvent.VK_L
  val M: KeyCode = KeyEvent.VK_M
  val N: KeyCode = KeyEvent.VK_N
  val O: KeyCode = KeyEvent.VK_O
  val P: KeyCode = KeyEvent.VK_P
  val Q: KeyCode = KeyEvent.VK_Q
  val R: KeyCode = KeyEvent.VK_R
  val S: KeyCode = KeyEvent.VK_S
  val T: KeyCode = KeyEvent.VK_T
  val U: KeyCode = KeyEvent.VK_U
  val V: KeyCode = KeyEvent.VK_V
  val W: KeyCode = KeyEvent.VK_W
  val X: KeyCode = KeyEvent.VK_X
  val Y: KeyCode = KeyEvent.VK_Y
  val Z: KeyCode = KeyEvent.VK_Z

  val alphabet = Vector(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z)

  def fromLetter(letter: Char): Option[KeyCode] =
    alphabet.zipWithIndex.map(_.swap).toMap.get(letter.toUpper.toByte - 'A'.toByte)

  val key0: KeyCode = KeyEvent.VK_0
  val key1: KeyCode = KeyEvent.VK_1
  val key2: KeyCode = KeyEvent.VK_2
  val key3: KeyCode = KeyEvent.VK_3
  val key4: KeyCode = KeyEvent.VK_4
  val key5: KeyCode = KeyEvent.VK_5
  val key6: KeyCode = KeyEvent.VK_6
  val key7: KeyCode = KeyEvent.VK_7
  val key8: KeyCode = KeyEvent.VK_8
  val key9: KeyCode = KeyEvent.VK_9

  val digits = Vector(key0, key1, key2, key3, key4, key5, key6, key7, key8, key9)

  def fromDigit(n: Int): Option[KeyCode] =
    digits.zipWithIndex.map(_.swap).toMap.get(n)

  val numpadDigits = Vector(
    KeyEvent.VK_NUMPAD0,
    KeyEvent.VK_NUMPAD1,
    KeyEvent.VK_NUMPAD2,
    KeyEvent.VK_NUMPAD3,
    KeyEvent.VK_NUMPAD4,
    KeyEvent.VK_NUMPAD5,
    KeyEvent.VK_NUMPAD6,
    KeyEvent.VK_NUMPAD7,
    KeyEvent.VK_NUMPAD8,
    KeyEvent.VK_NUMPAD9
  )

  val fKeys = Vector(
    KeyEvent.VK_F1,
    KeyEvent.VK_F2,
    KeyEvent.VK_F3,
    KeyEvent.VK_F4,
    KeyEvent.VK_F5,
    KeyEvent.VK_F6,
    KeyEvent.VK_F7,
    KeyEvent.VK_F8,
    KeyEvent.VK_F9,
    KeyEvent.VK_F10,
    KeyEvent.VK_F11,
    KeyEvent.VK_F12
  )

  val backslash: KeyCode = KeyEvent.VK_BACK_SLASH

  val ctrl: ModifierKey = ModifierKey(KeyEvent.VK_CONTROL)
  val shift: ModifierKey = ModifierKey(KeyEvent.VK_SHIFT)
  val alt: ModifierKey = ModifierKey(KeyEvent.VK_ALT)

  val enter: KeyCode = KeyEvent.VK_ENTER

  val space: KeyCode = KeyEvent.VK_SPACE
  val underscore: KeyCode = KeyEvent.VK_UNDERSCORE
  val dot: KeyCode = KeyEvent.VK_DECIMAL
  val colon: KeyCode = KeyEvent.VK_COLON
  val semiColon: KeyCode = KeyEvent.VK_SEMICOLON
  val dash: KeyCode = KeyEvent.VK_MINUS

}

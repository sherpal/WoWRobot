package be.doeraene.data

import akka.NotUsed
import be.doeraene.EntryPoint.{
  base64Bytes,
  baseToEncode64BytesIn,
  decodeByteGroupings,
  numberOfNumberOfBytesSquares,
  numberOfSquaresPerByte,
  sqrtOfNumberOfDataSquares,
  topLeft
}
import be.doeraene.models.GameState
import io.circe.generic.auto.*

import java.awt.{Color, Rectangle, Robot, Toolkit}
import scala.concurrent.duration.FiniteDuration
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.circe.{Json, JsonFloat}
import org.slf4j.{Logger, LoggerFactory}

import java.awt.image.BufferedImage
import scala.util.Try

/** The [[WOWGameStateProvider]] is responsible for decoding the game state from the Lua addon in WoW.
  *
  * The information is composed of three different parts:
  *   - One line with three squares (black - white - black) used to calibrate (infer) the size in pixels of the squares
  *   - One encoding the number of squares that are used afterwards (there are 4 squares which encodes this information)
  *   - A bunch of lines with all the squares that actually encode the game state.
  *
  * The game state is first encoded in JSON data, then this JSON is encoded as a base 64 string. The base 64 strings is
  * mapped to numbers from 0 to 63, which are then encoded in base 8 (two digits needed). In an RGB square, each channel
  * is used to encode precisely one of these 8 digits (hence, two squares encode 3 64-characters.
  *
  * @param topLeft
  */
final class WOWGameStateProvider(topLeft: (Int, Int)) extends Provider[Either[Throwable, GameState]] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  extension (x: Long) def pow(exp: Int): Long = if exp == 0 then 1L else x * (x pow (exp - 1))

  /** Maps a list of channel values (red, then green, then blue) to base 64 digits (from 0 to 63).
    */
  def decodeByteGroupings(bytes: List[Int]): List[Long] =
    bytes
      .grouped(numberOfSquaresPerByte)
      .map(
        _.map(_.toDouble / (256 / baseToEncode64BytesIn))
          .map(math.round)
          .reverse
          .zipWithIndex
          .map((b, exp) => b * (baseToEncode64BytesIn pow exp))
          .sum
      )
      .toList

  /** Finds the size of a square in pixel. */
  def inferSquarePixelSize(capture: BufferedImage): Int = {
    val line = (0 until 100)
      .map(xOffset => (topLeft._1 + xOffset, topLeft._2))
      .map { case (x, y) => new Color(capture.getRGB(x, y)) }
      .map(colour => List(colour.getRed, colour.getGreen, colour.getBlue).sum / 3)
      .toList

    val startWhiteSquare = line.indexWhere(_ > 128) // finding first light pixel
    val endWhiteSquare = line.indexWhere(_ < 128, startWhiteSquare) // finding first dark after first light

    endWhiteSquare - startWhiteSquare
  }

  /** Finds how many squares are used for encoding the game state. */
  def readWOWNumberOfBytes(capture: BufferedImage, squaresPixelSize: Int): Long = {
    val y = topLeft._2 + squaresPixelSize
    val numberOfBytesPositions = (0 until numberOfNumberOfBytesSquares * numberOfSquaresPerByte)
      .map(_ * squaresPixelSize)
      .map(xOffset => (topLeft._1 + xOffset, y))
      .toList

//    println(
//      numberOfBytesPositions
//        .map(position => new Color(capture.getRGB(position._1, position._2)))
//    )

    decodeByteGroupings(
      numberOfBytesPositions
        .map(position => new Color(capture.getRGB(position._1, position._2)))
        .flatMap(color => List(color.getRed, color.getGreen, color.getBlue))
    ).reverse.zipWithIndex.map((byte, exp) => byte * (64 pow exp)).sum
  }

  /** Decodes the JSON game state from the colour squares. */
  def readWOWBytes(numberOfBytes: Long, squaresPixelSize: Int, capture: BufferedImage) = {
    val rowSize = sqrtOfNumberOfDataSquares * numberOfSquaresPerByte
    val bytesPositions = (0 until (rowSize * sqrtOfNumberOfDataSquares))
      .map(squareIndex => ((squareIndex % rowSize) * squaresPixelSize, (squareIndex / rowSize) * squaresPixelSize))
      .map { (xOffset, yOffset) =>
        (topLeft._1 + xOffset, topLeft._2 + yOffset + 2 * squaresPixelSize)
      }
      .toList

    val totalNumberOfBytes = (numberOfBytes * numberOfSquaresPerByte).toInt
    val numberOfSquaresToTake =
      (totalNumberOfBytes / 3 + (if totalNumberOfBytes % 3 > 0 then 1L else 0L)).toInt

    //    bytesPositions.take(numberOfSquaresToTake).foreach { (x, y) =>
    //      robot.mouseMove(x, y)
    //      Thread.sleep(2000)
    //    }

//    println(
//      bytesPositions
//        .take(numberOfSquaresToTake)
//        .map(position => new Color(capture.getRGB(position._1, position._2)))
//    )

//    println(
//      decodeByteGroupings(
//        bytesPositions
//          .take(numberOfSquaresToTake)
//          .map(position => new Color(capture.getRGB(position._1, position._2)))
//          .flatMap(color => List(color.getRed, color.getGreen, color.getBlue))
//          .take(totalNumberOfBytes)
//      )
//    )

    val base64String = decodeByteGroupings(
      bytesPositions
        .take(numberOfSquaresToTake)
        .map(position => new Color(capture.getRGB(position._1, position._2)))
        .flatMap(color => List(color.getRed, color.getGreen, color.getBlue))
        .take(totalNumberOfBytes)
    )
      .map(_.toInt)
      .map(base64Bytes.apply)
      .mkString

    val decodedString = java.util.Base64.getDecoder.decode(base64String).map(_.toChar).mkString
    //println(decodedString)
    println(io.circe.parser.decode[io.circe.Json](decodedString).map(_.noSpaces))

    decodedString
  }

  def provide(): Either[Throwable, GameState] = for {
    capture <- Right {
      val robot = new Robot
      val screenRect = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)
      robot.createScreenCapture(screenRect)
    }
    squarePixelSize <- Try(inferSquarePixelSize(capture)).toEither
    numberOfBytes <- Try(readWOWNumberOfBytes(capture, squarePixelSize)).toEither
    decodedString <- Try(readWOWBytes(numberOfBytes, squarePixelSize, capture)).toEither
    gameState <- io.circe.parser.decode[GameState](decodedString)
  } yield gameState

}

object WOWGameStateProvider {

  def dummy: Provider[Either[Throwable, GameState]] = new Provider[Either[Throwable, GameState]] {
    val logger = LoggerFactory.getLogger(getClass)
    def provide() = Right(Json.fromInt(scala.util.Random.nextInt(10)))
  }
}

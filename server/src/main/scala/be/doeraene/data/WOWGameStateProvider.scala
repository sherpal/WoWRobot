package be.doeraene.data

import akka.NotUsed
import be.doeraene.EntryPoint.{
  base64Bytes,
  baseToEncode64BytesIn,
  decodeByteGroupings,
  numberOfNumberOfBytesSquares,
  numberOfSquaresPerByte,
  sqrtOfNumberOfDataSquares,
  squaresPixelSize,
  topLeft
}
import be.doeraene.models.GameState
import io.circe.generic.auto.*

import java.awt.{Color, Rectangle, Robot, Toolkit}
import scala.concurrent.duration.FiniteDuration
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.circe.{Json, JsonFloat}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

final class WOWGameStateProvider(topLeft: (Int, Int)) extends Provider[Either[Throwable, GameState]] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  extension (x: Long) def pow(exp: Int): Long = if exp == 0 then 1L else x * (x pow (exp - 1))

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

  def readWOWNumberOfBytes(): Long = {
    val numberOfBytesPositions = (0 until numberOfNumberOfBytesSquares * numberOfSquaresPerByte)
      .map(_ * squaresPixelSize)
      .map { xOffset =>
        (topLeft._1 + xOffset, topLeft._2)
      }
      .toList

    val robot = new Robot
    val screenRect = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)
    val capture = robot.createScreenCapture(screenRect)

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

  def readWOWBytes(numberOfBytes: Long) = {
    val rowSize = sqrtOfNumberOfDataSquares * numberOfSquaresPerByte
    val bytesPositions = (0 until (rowSize * sqrtOfNumberOfDataSquares))
      .map(squareIndex => ((squareIndex % rowSize) * squaresPixelSize, (squareIndex / rowSize) * squaresPixelSize))
      .map { (xOffset, yOffset) =>
        (topLeft._1 + xOffset, topLeft._2 + yOffset + squaresPixelSize)
      }
      .toList

    val totalNumberOfBytes = (numberOfBytes * numberOfSquaresPerByte).toInt
    val numberOfSquaresToTake =
      (totalNumberOfBytes / 3 + (if totalNumberOfBytes % 3 > 0 then 1L else 0L)).toInt

    val robot = new Robot
    val screenRect = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)
    val capture = robot.createScreenCapture(screenRect)

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
    numberOfBytes <- Try(readWOWNumberOfBytes()).toEither
    decodedString <- Try(readWOWBytes(numberOfBytes)).toEither
    gameState <- io.circe.parser.decode[GameState](decodedString)
  } yield gameState

}

object WOWGameStateProvider {

  def dummy: Provider[Either[Throwable, GameState]] = new Provider[Either[Throwable, GameState]] {
    val logger = LoggerFactory.getLogger(getClass)
    def provide() = Right(Json.fromInt(scala.util.Random.nextInt(10)))
  }
}

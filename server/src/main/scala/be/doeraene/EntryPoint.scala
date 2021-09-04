package be.doeraene

import com.typesafe.config.ConfigFactory

import java.awt.{List => _, _}
import java.awt.image.BufferedImage
import java.nio.file.Paths
import javax.imageio.ImageIO

object EntryPoint {

  extension (x: Long)
    def pow(exp: Int): Long = if exp == 0 then 1L else x * (x pow (exp - 1))

  lazy val config = ConfigFactory.parseFile(
    Paths.get("./addon/Puppet/Config.lua").toFile
  )

  lazy val squaresPixelSize = config.getInt("Puppet.config.squaresPixelSize")
  lazy val numberOfNumberOfBytesSquares =
    config.getInt("Puppet.config.numberOfNumberOfBytesSquares")
  lazy val sqrtOfNumberOfDataSquares =
    config.getInt("Puppet.config.sqrtOfNumberOfDataSquares")

  lazy val base64Bytes = config.getString("Puppet.config.base64Bytes")
  lazy val baseToEncode64BytesIn = config.getInt("Puppet.config.baseToEncode64BytesIn")
  lazy val numberOfSquaresPerByte = config.getInt("Puppet.config.numberOfSquaresPerByte")

  val topLeft = (216,60)

  def decodeByteGroupings(bytes: List[Int]): List[Long] =
    bytes.grouped(numberOfSquaresPerByte).map(_
      .map(_.toDouble / (256 / baseToEncode64BytesIn))
      .map(math.round)
      .reverse
      .zipWithIndex
      .map((b, exp) => b * (baseToEncode64BytesIn pow exp))
      .sum
    ).toList

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

    println(numberOfBytesPositions
      .map(position => new Color(capture.getRGB(position._1, position._2))))

    decodeByteGroupings(numberOfBytesPositions
      .map(position => new Color(capture.getRGB(position._1, position._2)))
      .flatMap(color => List(color.getRed, color.getGreen, color.getBlue)))
      .reverse
      .zipWithIndex.map((byte, exp) => byte * (64 pow exp))
      .sum
  }

  def readWOWBytes(numberOfBytes: Long) = {
    val bytesPositions = (0 until sqrtOfNumberOfDataSquares * numberOfSquaresPerByte)
      .map(_ * (squaresPixelSize))
      .map { xOffset =>
        (topLeft._1 + xOffset, topLeft._2 + squaresPixelSize)
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

    println(bytesPositions.take(numberOfSquaresToTake)
      .map(position => new Color(capture.getRGB(position._1, position._2))))

    println(decodeByteGroupings(bytesPositions.take(numberOfSquaresToTake)
      .map(position => new Color(capture.getRGB(position._1, position._2)))
      .flatMap(color => List(color.getRed, color.getGreen, color.getBlue)).take(totalNumberOfBytes))
      )

    val base64String = decodeByteGroupings(bytesPositions.take(numberOfSquaresToTake)
      .map(position => new Color(capture.getRGB(position._1, position._2)))
      .flatMap(color => List(color.getRed, color.getGreen, color.getBlue))
      .take(totalNumberOfBytes)
    )
      .map(_.toInt)
      .map(base64Bytes.apply)
      .mkString

    val decodedString = java.util.Base64.getDecoder.decode(base64String).map(_.toChar).mkString
    println(decodedString)
    println(io.circe.parser.decode[io.circe.Json](decodedString).map(_.spaces2))
  }

  def main(args: Array[String]): Unit = {
    val robot = new Robot
//    val screenRect = new Rectangle(Toolkit.getDefaultToolkit.getScreenSize)
//    val capture = robot.createScreenCapture(screenRect)
//    println(new Color(capture.getRGB(350, 300)).getRed)
//    println(capture.getRGB(200, 100))
//
//    Thread.sleep(10000)
//    val mousePosition = MouseInfo.getPointerInfo.getLocation
//    println((mousePosition.x, mousePosition.y))

    Thread.sleep(3000)
    val mousePosition = MouseInfo.getPointerInfo.getLocation
    println((mousePosition.x, mousePosition.y))

    robot.mouseMove(423 + squaresPixelSize,67)

    println(readWOWNumberOfBytes())
    readWOWBytes(16)
  }
}

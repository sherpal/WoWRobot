package be.doeraene.data.communication.arrayreader

import scala.collection.BuildFrom
import scala.collection.generic.CanBuildFrom
import scala.reflect.{ClassTag, TypeTest}
import scala.util.Try

/** An [[ArrayReader]] can extract, from an array, information for constructing an instance of T.
  */
trait ArrayReader[T] { self =>

  extension [U, A](either: Either[Throwable, (U, A)])
    def mapLeft[V](f: U => V): Either[Throwable, (V, A)] = either.map((u, a) => (f(u), a))

  def extractInfo(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, (T, Int)]

  final def extractInfoIgnoreCount(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, T] =
    extractInfo(rawInfo, startingIndex).map(_._1)

  def map[U](f: T => U): ArrayReader[U] = flatMap(f andThen ArrayReader.fromValue)
//    new ArrayReader[U] {
//    def extractInfo(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, (U, Int)] =
//      self.extractInfo(rawInfo, startingIndex).mapLeft(f)
//  }

  /** Uses this [[ArrayReader]] to read from the raw info, creates a new [[ArrayReader]] from the extracted result, then
    * read information using that created [[ArrayReader]], starting where the previous stopped.
    */
  def flatMap[U](f: T => ArrayReader[U]): ArrayReader[U] = new ArrayReader[U] {
    def extractInfo(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, (U, Int)] = for {
      selfResult <- self.extractInfo(rawInfo, startingIndex)
      (t, used) = selfResult
      that = f(t)
      thatResult <- that.extractInfo(rawInfo, startingIndex + used)
      (u, thatUsed) = thatResult
    } yield (u, used + thatUsed)
  }

}

object ArrayReader {

  @inline def apply[T](using ArrayReader[T]): ArrayReader[T] = summon[ArrayReader[T]]

  def fromValue[T](t: => T): ArrayReader[T] = new ArrayReader[T] {
    def extractInfo(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, (T, Int)] = Right((t, 0))
  }

  given [T](using auto: AutoArrayReader[T]): ArrayReader[T] = auto

  def of[T](using TypeTest[Any, T]): ArrayReader[T] = AutoArrayReader.of[T]

  given [T, M[_]](using tArrayReader: ArrayReader[T], buildFrom: BuildFrom[Vector[T], T, M[T]]): ArrayReader[M[T]] =
    //implicit def forVector[T](using tArrayReader: ArrayReader[T]): ArrayReader[Vector[T]] =
    new ArrayReader[M[T]] {
      def extractInfo(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, (M[T], Int)] =
        for {
          numberOfElements <- of[Int].extractInfoIgnoreCount(rawInfo, startingIndex)
          elements <- (1 to numberOfElements).foldLeft[Either[Throwable, (Vector[T], Int)]](Right((Vector(), 1))) {
            (maybeAcc, _) =>
              for {
                acc <- maybeAcc
                (currentElements, currentUsed) = acc
                next <- tArrayReader.extractInfo(rawInfo, startingIndex + currentUsed)
                (nextElement, nextUsed) = next
              } yield (currentElements :+ nextElement, currentUsed + nextUsed)
          }
        } yield (buildFrom.fromSpecific(Vector())(elements._1), elements._2)
    }

}

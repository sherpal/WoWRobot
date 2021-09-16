package be.doeraene.data.communication.arrayreader

import magnolia1.*

import scala.reflect.{ClassTag, TypeTest}
import scala.util.Try

trait AutoArrayReader[T] extends ArrayReader[T]

object AutoArrayReader extends AutoDerivation[AutoArrayReader] {

  def castAs[T](t: Any)(using TypeTest[Any, T]): Either[Throwable, T] = t match {
    case t: T => Right(t)
    case _    => Left(new ClassCastException(s"Value $t can't be cast to ${summon[TypeTest[Any, T]]}"))
  }

  def of[T](using TypeTest[Any, T]): AutoArrayReader[T] = new AutoArrayReader[T] {
    def extractInfo(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, (T, Int)] =
      for {
        elem <- Try(rawInfo(startingIndex)).toEither
        t <- castAs[T](elem)
      } yield (t, 1)
  }

  given AutoArrayReader[String] = of[String]
  given AutoArrayReader[Int] = of[Int]
  given AutoArrayReader[Double] = new AutoArrayReader[Double] {
    def extractInfo(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, (Double, Int)] =
      of[Double].extractInfo(rawInfo, startingIndex) match {
        case Right(value) => Right(value)
        case Left(_)      => of[Int].map(_.toDouble).extractInfo(rawInfo, startingIndex)
      }
  }
  given AutoArrayReader[Boolean] = of[Boolean]

  def join[T](ctx: CaseClass[AutoArrayReader, T]): AutoArrayReader[T] = new AutoArrayReader[T] {
    def extractInfo(rawInfo: Vector[Any], startingIndex: Int): Either[Throwable, (T, Int)] = {
      val maybeFields: Either[Throwable, (Seq[Any], Int)] =
        ctx.params.foldLeft(Right[Throwable, (Seq[Any], Int)]((Nil, 0))) {
          (maybeAcc: Either[Throwable, (Seq[Any], Int)], next) =>
            for {
              acc <- maybeAcc
              (currentFields, currentlyUsed) = acc
              nextResult <- next.typeclass.extractInfo(rawInfo, startingIndex + currentlyUsed)
              (nextField, nextUsed) = nextResult
            } yield (currentFields :+ nextField, currentlyUsed + nextUsed)
        }

      maybeFields.flatMap((fields, indicesUsed) => Try((ctx.rawConstruct(fields), indicesUsed)).toEither)

    }

  }

  def split[T](ctx: SealedTrait[AutoArrayReader, T]): AutoArrayReader[T] = ???

}

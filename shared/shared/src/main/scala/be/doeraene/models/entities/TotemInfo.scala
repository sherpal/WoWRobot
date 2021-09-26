package be.doeraene.models.entities

sealed trait TotemInfo:
  def index: Int
  def isPresent: Boolean

object TotemInfo:
  case class AliveTotemInfo(index: Int, name: String, startTime: Int, duration: Int) extends TotemInfo:
    def isPresent: Boolean = true

  case class AbsentTotemInfo(index: Int) extends TotemInfo:
    def isPresent: Boolean = false

  val empty: Vector[TotemInfo] = (1 to 4).map(AbsentTotemInfo(_)).toVector

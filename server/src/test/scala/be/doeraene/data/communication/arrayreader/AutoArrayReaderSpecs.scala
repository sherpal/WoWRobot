package be.doeraene.data.communication.arrayreader

final class AutoArrayReaderSpecs extends munit.FunSuite {

  case class DemoCaseClass(foo: String, b: Boolean, i: Int)

  case class NestedDemoCaseClass(foo: String, demoCaseClass: DemoCaseClass, x: Double)

  test("Auto Reading a small case class") {
    assertEquals(
      ArrayReader[DemoCaseClass].extractInfo(Vector("hello", true, 3), 0),
      Right((DemoCaseClass("hello", true, 3), 3))
    )
  }

  test("Auto Reading a small nested case class") {

    assertEquals(
      ArrayReader[NestedDemoCaseClass].extractInfo(
        Vector("stuff", "hello", true, 3, 3),
        0
      ),
      Right((NestedDemoCaseClass("stuff", DemoCaseClass("hello", true, 3), 3), 5))
    )
  }

  test("Reading as a List") {
    val data = Vector(2, "hello", true, 3, "hi", false, 4)
    assertEquals(
      ArrayReader[Vector[DemoCaseClass]].extractInfo(data, 0),
      Right((Vector(DemoCaseClass("hello", true, 3), DemoCaseClass("hi", false, 4)), 7))
    )
  }

  test("Failing to read when not enough elements") {
    assert(ArrayReader[DemoCaseClass].extractInfo(Vector("hello"), 0).isLeft)
  }

  test("Failing when wrong type") {
    assert(ArrayReader[DemoCaseClass].extractInfo(Vector("hello", "hello", 3), 0).isLeft)
  }

  test("Mapping with identity changes nothing") {
    val info = Vector("hello", true, 3)
    assertEquals(
      ArrayReader[DemoCaseClass].map(identity).extractInfo(info, 0),
      ArrayReader[DemoCaseClass].extractInfo(info, 0)
    )
  }

}

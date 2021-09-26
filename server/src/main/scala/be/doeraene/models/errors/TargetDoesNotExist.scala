package be.doeraene.models.errors

final class TargetDoesNotExist(val index: Int) extends RuntimeException(s"The target with index $index does not exist.")

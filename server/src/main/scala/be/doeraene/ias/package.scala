package be.doeraene

import be.doeraene.models.errors.TargetDoesNotExist
import be.doeraene.services.robot.*
import be.doeraene.services.robot.Keyboard.*
import zio.{UIO, ZIO}

package object ias {

  def targetGroupMember(index: Int) =
    ZIO.ifM(UIO(index >= 5))(ZIO.fail(new TargetDoesNotExist(index)), pressAndReleaseKey(fKeys(index)))

}

package be.doeraene

import be.doeraene.models.errors.TargetDoesNotExist
import be.doeraene.services.robot.*
import be.doeraene.services.robot.Keyboard.*
import zio.ZIO

package object ias {

  def targetGroupMember(index: Int) =
    if (index >= 5) ZIO.fail(new TargetDoesNotExist(index))
    else pressAndReleaseKey(fKeys(index))

}

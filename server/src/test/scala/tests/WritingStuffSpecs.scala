package tests

import be.doeraene.services.robot._
import be.doeraene.services.robot.Keyboard._
import zio.duration._

import be.doeraene.ias.paladin.ret.lowlevel._

final class WritingStuffSpecs extends munit.FunSuite {

  test("Writing stuff") {

    be.doeraene.ZIOConfig.theRuntime.unsafeRun(
      zio.clock.sleep(2.seconds) *>
        (for {
          _ <- castJudgement
          _ <- castSealOfRighteousness
          _ <- zio.clock.sleep(1.second)
        } yield ()).repeatN(10)
    )

  }

}

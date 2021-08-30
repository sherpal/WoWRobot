package tests

import com.typesafe.config.{ConfigFactory, ConfigParseOptions}

import java.nio.file.Paths

final class ConfigSpecs extends munit.FunSuite {

  test("Reading Lua file as config works") {
    val config = ConfigFactory.parseFile(
      Paths.get("./addon/Puppet/Config.lua").toFile
    )

    val squaresPixelSize = config.getInt("Puppet.config.squaresPixelSize")
    assert(squaresPixelSize > 0)
    val sqrtOfNumberOfDataSquares =
      config.getInt("Puppet.config.sqrtOfNumberOfDataSquares")
    assert(sqrtOfNumberOfDataSquares > 0)
  }

}

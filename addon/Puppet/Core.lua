print("Welcome, puppet!")

local collection = Puppet.collection
local LIO = Puppet.LIO
local drawing = Puppet.GameStateDrawing
local json = Puppet.JsonSerializer

local makeJson = json.makeJsonSerializableAuto


local gameState = makeJson({
  empty = true
})


-- testing
LIO.runToFuture(drawing.prepareDrawingEffect:delayed(1):flatMap(function(squares)
  return LIO.fromFunction(function ()
    local mana = UnitPower("player", UnitPowerType("player"))

    return json.makeJsonSerializableAuto({
      mana = mana
    })
  end):flatMap(function(gameState)
    return drawing.drawState(squares, gameState)
  end):repeatEvery(5)
end))


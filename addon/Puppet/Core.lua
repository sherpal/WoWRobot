print("Welcome, puppet!")

local collection = Puppet.collection
local LIO = Puppet.LIO
local drawing = Puppet.GameStateDrawing
local json = Puppet.JsonSerializer

local makeJson = json.makeJsonSerializableAuto


local gameState = {
  result = 0
}

local accessGameState = LIO.fromFunction(function() return makeJson(gameState) end)

local prepareInOneSec = drawing.prepareDrawingEffect:delayed(1)

local function drawGameState(squares)
  return LIO.fromFunction(function()
    print(tostring(gameState.result))
  end):thenRun(accessGameState):flatMap(function(gs)
    return drawing.drawState(squares, gs)
  end):repeatEvery(1):fork()
end

local updateGameState = LIO.fromFunction(function ()
  gameState = {result = math.random(10)}
end):repeatEvery(1)

local effect = prepareInOneSec:flatMap(drawGameState):thenRun(updateGameState)

LIO.runToFuture(effect:fork())

-- testing
-- this was an example showing a game state displaying mana
-- LIO.runToFuture(drawing.prepareDrawingEffect:delayed(1):flatMap(function(squares)
--   return LIO.fromFunction(function ()
--     local mana = UnitPower("player", UnitPowerType("player"))

--     return json.makeJsonSerializableAuto({
--       mana = mana
--     })
--   end):flatMap(function(gameState)
--     return drawing.drawState(squares, gameState)
--   end):repeatEvery(5)
-- end))


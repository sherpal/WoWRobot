print("Welcome, puppet!")

local collection = Puppet.collection
local LIO = Puppet.LIO
local drawing = Puppet.GameStateDrawing


local gameState = {
  usedAbilities = {}
}

local accessGameState = LIO.fromFunction(function() return gameState end)

local prepareInOneSec = drawing.prepareDrawingEffect:delayed(1)

local function drawGameState(squares)
  return accessGameState:flatMap(function(gs)
    return drawing.drawState(squares, gs)
  end):repeatEvery(1):fork()
end

local updateGameState = LIO.fromFunction(function ()
  --gameState = {result = math.random(10)}
end):repeatEvery(1)

local effect = prepareInOneSec:flatMap(drawGameState):thenRun(updateGameState)

LIO.runToFuture(effect:fork())

------------------------
-- Handling of Events --
------------------------

local eventFrame = CreateFrame("Frame")
eventFrame:RegisterEvent("UNIT_SPELLCAST_SUCCEEDED")

eventFrame.callbacks = {
  UNIT_SPELLCAST_SUCCEEDED = function(unitTarget, castGUID, spellID)
    Puppet.Core.changeGameState(function(currentGS)
      local name, _, _, castTime, minRange, maxRange, _ = GetSpellInfo(spellID)
      currentGS.usedAbilities[spellID] = collection.new({unitTarget, castGUID, GetTime()}):concat(
        collection.new({name, castTime or 0, minRange or 0, maxRange or 0})
      )
      return currentGS
    end)
  end
}

eventFrame:SetScript("OnEvent", function(self, event, ...)
  (self.callbacks[event] or function() end)(...)
end)

local registerEventCallback = function(event, callback)
  eventFrame.callbacks[event] = callback
end

Puppet.Scheduler.setInterval(0, 1, function()
  Puppet.Core.changeGameState(function(gs)
    gs.health = {
      player = UnitHealth("player")
    }
    return gs
  end)
end, "healths-updater")

Puppet.Core = {
  registerEventCallback = registerEventCallback,
  changeGameState = function(update)
    -- updates the game state with the given function update: GameState => GameState
    gameState = update(gameState)
  end
}

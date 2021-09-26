print("Welcome, puppet!")

local collection = Puppet.collection
local LIO = Puppet.LIO
local drawing = Puppet.GameStateDrawing


local gameState = {
  inCombat = false,
  usedAbilities = collection.empty,
  playerBuffs = collection.empty,
  playersInfo = collection.empty
}

local accessGameState = LIO.fromFunction(function() return gameState end)

local prepareInOneSec = drawing.prepareDrawingEffect:delayed(1)

local function drawGameState(squares)
  return accessGameState:flatMap(function(gs)
    return drawing.drawState(squares, gs)
  end):repeatEvery(0.3):fork()
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
eventFrame:RegisterEvent("PLAYER_REGEN_DISABLED")
eventFrame:RegisterEvent("PLAYER_REGEN_ENABLED")

eventFrame.callbacks = {
  UNIT_SPELLCAST_SUCCEEDED = function(unitTarget, castGUID, spellID)
    Puppet.Core.changeGameState(function(currentGS)
      local name, _, _, castTime, minRange, maxRange, _ = GetSpellInfo(spellID)
      local start, duration, enabled = GetSpellCooldown(spellID)
      currentGS.usedAbilities = currentGS.usedAbilities:filter(function(info)
        return info[4] ~= spellID
      end):append(collection.new({unitTarget or "none", castGUID, GetTime()}):concat(
        collection.new({
          spellID, name, castTime or 0, minRange or 0, maxRange or 0,
          start, duration, enabled == 1
        })
      ))
      return currentGS
    end)
  end,
  PLAYER_REGEN_DISABLED = function()
    Puppet.Core.changeGameState(function(currentGS)
      currentGS.inCombat = true
      return currentGS
    end)
  end,
  PLAYER_REGEN_ENABLED = function()
    Puppet.Core.changeGameState(function(currentGS)
      currentGS.inCombat = false
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

local function buildPlayerBuffs()
  local result = {}
  local buffIndex = 1
  while true do
    local name, _, count, debuffType, duration, expirationTime, source, _, 
    _, spellId = UnitBuff("player", buffIndex)
    if type(name) == "nil" then
      break
    end
    result[buffIndex] = collection.new({
      name, count, debuffType or "", duration or -1, expirationTime or -1, source or "none", spellId
    })

    buffIndex = buffIndex + 1
  end

  return collection.new(result)
end

Puppet.Scheduler.setInterval(0, 0.25, function()
  Puppet.Core.changeGameState(function(gs)

    local units = collection.single("player"):concat(
      collection.range(1, 4):map(function (idx)
        return "party" .. tostring(idx)
      end)
    )

    gs.playersInfo = units:map(function (unit)
      return collection.new({UnitName(unit), UnitHealth(unit), UnitHealthMax(unit)})
    end):filter(function(info) return info[3] > 0 end)

    gs.usedAbilities = gs.usedAbilities:map(function(info)
      local staticInfo = info:take(info:length() - 3)
      local spellID = staticInfo[4]
      local start, duration, enabled = GetSpellCooldown(spellID)
      return staticInfo:concat(collection.new({start, duration, enabled == 1}))
    end)

    gs.playerBuffs = buildPlayerBuffs()

  end)
end, "healths-updater")

Puppet.Scheduler.setInterval(30, 30, function()
  Puppet.Core.changeGameState(function(gs)
    local now = GetTime()
    gs.usedAbilities:filter(function(info)
      -- keeping only spell that where cast less than 10 minutes ago.
      return now - info[3] < 600
    end)
  end)
end)

Puppet.Core = {
  registerEventCallback = registerEventCallback,
  changeGameState = function(update)
    -- updates the game state with the given function update: GameState => GameState
    update(gameState)
  end
}

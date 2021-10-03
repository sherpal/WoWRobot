local collection = Puppet.collection

local _, _, classId = UnitClass("player")

if classId ~= Puppet.config.classIds.Shaman then
  print("Current class is not a shaman")
  return
end

print("AI for sham heal")

Puppet.Core.changeGameState(function(gs)
  gs.classId = classId
  return gs
end)

Puppet.Scheduler.setInterval(0, 0.25, function()
  Puppet.Core.changeGameState(function(gs)
    gs.totems = collection.range(1, 4):map(function (totemIndex)
      local haveTotem, totemName, startTime, duration = GetTotemInfo(totemIndex)
      return collection.new({
        totemIndex, haveTotem, totemName or "", startTime or 0, duration or 0
      })
    end)
  end)
end)

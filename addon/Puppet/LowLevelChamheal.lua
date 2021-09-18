

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

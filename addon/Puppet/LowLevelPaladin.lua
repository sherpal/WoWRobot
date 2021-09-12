

local _, _, classId = UnitClass("player")

if classId ~= Puppet.config.classIds.Paladin then
  print("Current class is not a paladin")
  return
end

print("AI for paladin")

Puppet.Core.changeGameState(function(gs)
  gs.classId = classId
  return gs
end)

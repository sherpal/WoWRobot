print("Welcome, puppet!")

local WorldFrame = Puppet.WorldFrame
local collection = Puppet.collection

local function createSquares(pixelSize, howMany)
  local squares = collection.empty:padTo(0, howMany):map(function()
    local square = CreateFrame("Button", nil, WorldFrame)
    square:SetSize(pixelSize, pixelSize)
    square.tex = square:CreateTexture()
    square.tex:SetAllPoints()
    square.tex:SetColorTexture(1,0,1,1)
    return square
  end)

  squares.values[1]:SetPoint("TOPLEFT", WorldFrame, "TOPLEFT")

  squares:zipWithTail():foreach(function(elem)
    local left = elem[1]
    local right = elem[2]
    right:SetPoint("LEFT", left, "RIGHT")
  end)

  return squares
end


local squares = createSquares(10, 100)

print(squares:length())

function Puppet.encodeLife()
  local mana = UnitPower("player", UnitPowerType("player"))

  local gameState = Puppet.JsonSerializer.makeJsonSerializable({
    mana = mana
  }, Puppet.collection.single("mana"))

  print(gameState:toJson())

  Puppet.GameStateDrawing.drawState(squares, gameState)
  
end




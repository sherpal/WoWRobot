
local drawing = {}
Puppet.GameStateDrawing = drawing

local json = Puppet.JsonSerializer
local base64 = Puppet.base64
local LIO = Puppet.LIO
local collection = Puppet.collection

local WorldFrame = Puppet.WorldFrame
local pixelSize = Puppet.config.squaresPixelSize

local effect = LIO.fromFunction(function()
  print("Preparing GameStateDrawing")
  local mainFrame = CreateFrame("Frame")
  mainFrame:SetPoint("TOPLEFT", WorldFrame, "TOPLEFT")
  local mainFrameSize = (Puppet.config.sqrtOfNumberOfDataSquares + 1) * pixelSize
  mainFrame:SetSize(mainFrameSize, mainFrameSize)

  local numberOfBytesFrame = CreateFrame("Frame", "NumberOfBytes", mainFrame)
  numberOfBytesFrame:SetPoint("TOPLEFT", mainFrame, "TOPLEFT")
  numberOfBytesFrame:SetSize(mainFrameSize, pixelSize)

  local dataBytesFrame = CreateFrame("Frame", "DataBytes", mainFrame)
  dataBytesFrame:SetPoint("TOPLEFT", numberOfBytesFrame, "BOTTOMLEFT")
  local dataBytesFrameSize = Puppet.config.sqrtOfNumberOfDataSquares * pixelSize
  dataBytesFrame:SetSize(dataBytesFrameSize, dataBytesFrameSize)

  local createSquare = function(parentFrame)
    local square = CreateFrame("Button", nil, parentFrame)
    square:SetSize(pixelSize, pixelSize)
    square.tex = square:CreateTexture()
    square.tex:SetAllPoints()
    square.tex:SetColorTexture(0,0,0,1)
    return square
  end

  local numberOfBytesSquares = collection.empty:padTo(
    numberOfBytesFrame, Puppet.config.numberOfNumberOfBytesSquares
  ):map(createSquare)

  do
    local leftSquare = numberOfBytesSquares[1]
    leftSquare:SetPoint("TOPLEFT", numberOfBytesFrame, "TOPLEFT")
    numberOfBytesSquares:zipWithTail():foreach(function(elem)
      local left = elem[1]
      local right = elem[2]
      right:SetPoint("LEFT", left, "RIGHT")
    end)
  end

  local dataBytesSquares = collection.empty:padTo(
    dataBytesFrame,
    Puppet.config.sqrtOfNumberOfDataSquares * Puppet.config.sqrtOfNumberOfDataSquares
  ):map(createSquare)

  do
    local leftSquare = dataBytesSquares[1]
    leftSquare:SetPoint("TOPLEFT", dataBytesFrame, "TOPLEFT")

    local squaresGrouped = dataBytesSquares:grouped(Puppet.config.sqrtOfNumberOfDataSquares)

    squaresGrouped[1]:zipWithTail():foreach(function(elem)
      local left = elem[1]
      local right = elem[2]
      right:SetPoint("LEFT", left, "RIGHT")
    end)

    squaresGrouped:zipWithTail():foreach(function(elem)
      local lineAbove = elem[1]
      local lineBelow = elem[2]

      lineAbove:zip(lineBelow):foreach(function(squaresTopAndBottom)
        local top = squaresTopAndBottom[1]
        local bottom = squaresTopAndBottom[2]
        bottom:SetPoint("TOP", top, "BOTTOM")
      end)
    end)
  end

  return {numberOfBytesSquares = numberOfBytesSquares, dataBytesSquares = dataBytesSquares}
end)

local drawNumberOfBytes = function(numberOfBytesSquares, squares)
  return LIO.fromFunction(function()
    local numberOfDigits = squares:length() * 3
    local base64Repr = Puppet.utils.baseN(numberOfBytesSquares, 64):padLeftTo(0, numberOfDigits)

    squares:zip(base64Repr:map(function (d)
      return d * 4 / 255
    end):grouped(3)):foreach(function(squareAndColor)
      local square = squareAndColor[1]
      local rgb = squareAndColor[2]
      square.tex:SetColorTexture(rgb[1], rgb[2], rgb[3], 1)
    end)
  end)
end

local drawDataBytes = function(base64Bytes, squares)
  return LIO.fromFunction(function()
    -- take while is an optimization
    local numberOfNonTransparentSquares = squares:takeWhile(function (square)
      return square.tex:GetAlpha() > 0
    end):length()

    local squaresToModify = squares:take(math.max(numberOfNonTransparentSquares, math.ceil(base64Bytes:length() / 3)))

    local transparent = {0, 0, 0, 0}
    local paddedColours = base64Bytes:map(function (x) return x * 3 end):grouped(3):padTo(
      transparent, squaresToModify:length()
    )

    squaresToModify:zip(paddedColours):foreach(function (squareAndColour)
      local square = squareAndColour[1]
      local rgb    = squareAndColour[2]
      rgb[2] = rgb[2] or 0
      rgb[3] = rgb[3] or 0
      rgb[4] = rgb[4] or 1

      square.tex:SetColorTexture(rgb[1] / 256, rgb[2] / 256, rgb[3] / 256, rgb[4])
    end)
      
  end)
end

local function drawState(squares, gameState)
  return LIO.unit(base64.encodeToNumber(json.toJson(gameState))):flatMap(function(encodedData)
    return LIO.runAll(collection.new({
      drawNumberOfBytes(encodedData:length(), squares.numberOfBytesSquares),
      drawDataBytes(encodedData, squares.dataBytesSquares)
    }))
  end)
end

-- testing
LIO.runToFuture(effect:delayed(1):flatMap(function(squares)
  return LIO.fromFunction(function ()
    local mana = UnitPower("player", UnitPowerType("player"))

    return json.makeJsonSerializableAuto({
      mana = mana
    })
  end):flatMap(function(gameState)
    print(gameState:toJson())
    return drawState(squares, gameState)
  end):repeatEvery(1)
end))


drawing.drawState = drawState

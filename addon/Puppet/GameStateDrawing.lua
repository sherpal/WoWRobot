
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

  local numberOfBytesSquares = collection.range(1, Puppet.config.numberOfNumberOfBytesSquares):map(function()
    local square = CreateFrame("Button", nil, numberOfBytesFrame)
    square:SetSize(pixelSize, pixelSize)
    square.tex = square:CreateTexture()
    square.tex:SetAllPoints()
    square.tex:SetColorTexture(1,0,1,1)
    return square
  end)

  do
    local leftSquare = numberOfBytesSquares[1]
    leftSquare:SetPoint("TOPLEFT", numberOfBytesFrame, "TOPLEFT")
    numberOfBytesSquares:zipWithTail():foreach(function(elem)
      local left = elem[1]
      local right = elem[2]
      right:SetPoint("LEFT", left, "RIGHT")
    end)
  end

  return numberOfBytesSquares
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

LIO.runToFuture(effect:delayed(1):flatMap(function(numberOfBytesSquares)
    return LIO.fromFunction(function ()
      return math.random(0, 64*64*64)
    end):flatMap(function (n)
      return drawNumberOfBytes(n, numberOfBytesSquares)
    end):repeatEvery(1)
  end))

function drawing.drawState(squares, gameState)
  local encodedData = base64.encodeToNumber(json.toJson(gameState))

  encodedData:map(function(value) return value * 4 end):grouped(3):zip(squares):foreach(
    function(rgbAndSquare)
      local rgb = rgbAndSquare[1]
      local square = rgbAndSquare[2]

      rgb[2] = rgb[2] or 0
      rgb[3] = rgb[3] or 0
      
      square.tex:SetColorTexture(rgb[1] / 256, rgb[2] / 256, rgb[3] / 256, 1)
    end
  )
end

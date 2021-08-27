
local drawing = {}
Puppet.GameStateDrawing = drawing

local json = Puppet.JsonSerializer
local base64 = Puppet.base64

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

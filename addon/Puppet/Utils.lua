
------------------------
-- Json serialization --
------------------------

local serializer = {}

serializer.mt = {}
serializer.mt.__index = serializer.mt

function serializer.inherit(element)
  local theMetaTable = element
  local result

  while theMetaTable do
    result = theMetaTable
    theMetaTable = getmetatable(result)
  end

  setmetatable(result, serializer.mt)
end

-- Makes the given element serializable, with the given fields to encode
-- fields must be a collection of strings.
function serializer.makeJsonSerializable(element, fields)
  serializer.inherit(element)
  function element:keysToJsonEncode()
    return fields
  end

  return element
end

serializer.mt.isSerializable = true

-- The default implementation of the json encoding requires that the element
-- defines the list of its keys it wants to encode
-- See also makeJsonSerializable
function serializer.mt:keysToJsonEncode()
  error("Not Implemented: keysToJsonEncode")
end

-- Convert itself to a json string
function serializer.mt:toJson()

  local keys = self:keysToJsonEncode()

  return keys:map(function (key)
    return "\"" .. key .. "\":" .. serializer.toJson(self[key])
  end):mkString("{", ",", "}")
end

-- Convert the given element to a json string
function serializer.toJson(element)
  -- todo: escape characters
  if type(element) == "string" then return "\"" .. element .. "\"" end
  if type(element) ~= "table"  then return tostring(element)       end
  if element.isSerializable    then return element:toJson()        end
  error("Element is not serializable: " .. tostring(element))
end


---------------------
-- Base64 encoding --
---------------------

local base64 = {}

-- character table string
local b='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'

-- encoding
function base64.encode(data)
    return ((data:gsub('.', function(x) 
        local r,b='',x:byte()
        for i=8,1,-1 do r=r..(b%2^i-b%2^(i-1)>0 and '1' or '0') end
        return r;
    end)..'0000'):gsub('%d%d%d?%d?%d?%d?', function(x)
        if (#x < 6) then return '' end
        local c=0
        for i=1,6 do c=c+(x:sub(i,i)=='1' and 2^(6-i) or 0) end
        return b:sub(c+1,c+1)
    end)..({ '', '==', '=' })[#data%3+1])
end

-- decoding
function base64.decode(data)
    data = string.gsub(data, '[^'..b..'=]', '')
    return (data:gsub('.', function(x)
        if (x == '=') then return '' end
        local r,f='',(b:find(x)-1)
        for i=6,1,-1 do r=r..(f%2^i-f%2^(i-1)>0 and '1' or '0') end
        return r;
    end):gsub('%d%d%d?%d?%d?%d?%d?%d?', function(x)
        if (#x ~= 8) then return '' end
        local c=0
        for i=1,8 do c=c+(x:sub(i,i)=='1' and 2^(8-i) or 0) end
        return string.char(c)
    end))
end




-- exports
Puppet.JsonSerializer = serializer
Puppet.base64 = base64

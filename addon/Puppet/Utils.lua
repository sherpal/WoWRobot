
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

function serializer.makeJsonSerializableAuto(element)
  local fields = {}
  for k, v in pairs(element) do
    if type(v) ~= "function" then fields[#fields+1] = k end
    if type(v) == "table" and (not v.isSerializable) then serializer.makeJsonSerializableAuto(v) end
  end

  return serializer.makeJsonSerializable(element, Puppet.collection.new(fields))
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

-- Dumps the element into a JSON string, using the automatic serializer
-- if the element is not an instance of Serializable
function serializer.toJsonAuto(element)
  return (element.isSerializable and element or serializer.makeJsonSerializableAuto(element)):toJson()
end

-- Convert the given element to a json string
function serializer.toJson(element)
  -- todo: escape characters
  if type(element) == "nil"    then return "null"                  end
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

base64.bytes = b

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

-- Takes the string data and encoded as base 64 bytes, using numbers from 0 to 63
function base64.encodeToNumber(data)
  return Puppet.collection.charsFromString(base64.encode(data)):map(function(char)
    if char ~= "=" then
      return b:find(char) - 1
    end
  end):filter(function(elem) return elem and true or false end)
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

-------------------------------------
--- Number base repr ---
------------------------

local floor, insert = math.floor, table.insert
local function baseN(n, base)
  local b = base
  n = floor(n)
  local t = {}
  repeat
      local d = (n % b)
      n = floor(n / b)
      insert(t, 1, d)
  until n == 0
  return Puppet.collection.new(t)
end


-- exports
Puppet.JsonSerializer = serializer
Puppet.base64 = base64
Puppet.identity = function(x) return x end
Puppet.utils = {
  baseN = baseN
}

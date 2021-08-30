
local collection = {}

collection.mt = {}
collection.mt.__index = function (col, key)
  if type(key) == "number" then
    return col.values[key]
  else
    return collection.mt[key]
  end
end

function collection.mt:toJson()
  return self:map(Puppet.JsonSerializer.toJson):mkString("[", ",", "]")
end

Puppet.JsonSerializer.inherit(collection.mt)

collection.mt.__eq = function(left, right)
  return left:length() == right:length() and left:zip(right):forall(function(pair)
    return pair[1] == pair[2]
  end)
end

collection.mt.__tostring = function(col) return col:toString() end

function collection.mt:append(element)
  local result = {}
  local length = self:length()

  for j = 1, length do
    result[j] = self[j]
  end
  result[length + 1] = element

  return collection.new(result)
end

-- Returns the length of the collection
function collection.mt:length()
  return #self.values
end

-- Returns true if at least one element satisfy the predicate
-- false for empty collection
function collection.mt:exists(predicate)
  return self:foldLeft(false, function (acc, elem) return acc or predicate(elem) end)
end

function collection.mt:foldLeft(initial, f)
  local result = initial
  self:foreach(function(element)
    result = f(result, element)
  end)
  return result
end

-- Returns whether all elements in the collection satisfy the predicate
-- true for empty collection
function collection.mt:forall(predicate)
  return self:foldLeft(true, function(acc, elem) return acc and predicate(elem) end)
end

-- Applies the effectfull function f to each element of the collection
function collection.mt:foreach(f)
  for _, elem in ipairs(self.values) do
    f(elem)
  end
end

-- Maps each element of the collection via f, creating a new collection
function collection.mt:map(f)
  local result = {}
  self:foreach(function(elem) result[#result+1] = f(elem) end)
  return collection.new(result)
end

function collection.mt:mkString(before, seperator, after)
  if type(before) == "nil" then
    return self:mkString("")
  end

  if type(seperator) == "nil" and type(after) == "nil" then
    return self:mkString("", before, "")
  end

  if type(after) == "nil" then
    error("mkString takes one or three arguments, not two.")
  end

  return before .. table.concat(self:map(tostring).values, seperator) .. after
end

-- Maps each element of the collection via f, flattening the result
-- f must return a collection
function collection.mt:flatMap(f)
  local result = {}
  local j = 0
  self:foreach(function(elem)
    f(elem):foreach(function(elemToAdd)
      j = j + 1
      result[j] = elemToAdd
    end)
  end)
  return collection.new(result)
end

function collection.mt:flatten()
  return self:flatMap(Puppet.identity)
end

-- Returns only the element satisfying the predicate
function collection.mt:filter(predicate)
  return self:flatMap(function(x)
    return predicate(x) and collection.single(x) or collection.empty
  end)
end

-- Returns only the element *not* satisfying the predicate
function collection.mt:filterNot(predicate)
  return self:filter(function(element) return not predicate(element) end)
end

-- Groups the elements in the collection by groupSize
-- The first element of the returned collection contains the groupSize first element of
-- this collection. The last group is truncated if need be.
function collection.mt:grouped(groupSize)
  if groupSize <= 0 then error("Group size must be positive", 2) end
  -- we do this like that because it seems more optimized.
  local groups = {}
  local groupId = 1
  local currentlyInGroup = 0
  self:foreach(function(element)
    if currentlyInGroup >= groupSize then
      currentlyInGroup = 0
      groupId = groupId + 1
    end
    currentlyInGroup = currentlyInGroup + 1

    groups[groupId] = groups[groupId] or {}
    groups[groupId][currentlyInGroup] = element
  end)

  return collection.new(groups):map(collection.new)
end

function collection.mt:isEmpty()
  return #self.values == 0
end

function collection.mt:sum(zero)
  return self:foldLeft(zero or 0, function(left, right) return left + right end)
end

function collection.mt:toString()
  return self:mkString(", ")
end

-- Fills the collection with the padding element from the left, until it reaches desired length
function collection.mt:padLeftTo(paddingElement, desiredLength)
  local result = {}
  local numberToFill = desiredLength - self:length()
  if numberToFill <= 0 then return self end
  for j = 1, desiredLength do
    result[j] = j <= numberToFill and paddingElement or self[j - numberToFill]
  end
  return collection.new(result)
end

-- Fills the collection with the padding element, until it reaches desired length
function collection.mt:padTo(paddingElement, desiredLength)
  local result = {}
  for j = 1, desiredLength do
    result[j] = self.values[j] or paddingElement
  end
  return collection.new(result)
end

function collection.mt:partition(predicate)
  local predicateSatisfied = {}
  local predicateNotSatisfied = {}

  self:foreach(function(elem)
    if predicate(elem) then
      predicateSatisfied[#predicateSatisfied+1] = elem
    else
      predicateNotSatisfied[#predicateNotSatisfied+1] = elem
    end
  end)

  return collection.new(predicateSatisfied), collection.new(predicateNotSatisfied)
end

-- Returns the indices of the collection (starting at 1, like lua wants it)
function collection.mt:indices()
  local result = {}
  for j = 1, self:length() do
    result[j] = j
  end
  return collection.new(result)
end

function collection.mt:tail()
  return self:zipWithIndex():filter(function(elem)
    return elem[2] > 1
  end):map(function(elem) return elem[1] end)
end

-- take the first number elements of the list, or the entire list if its length is
-- smaller.
function collection.mt:take(number)
  local actualLength = math.min(number, self:length())
  local result = {}
  for j = 1, actualLength do
    result[j] = self[j]
  end
  return collection.new(result)
end

-- Take elements in the collection while the predicate is true
function collection.mt:takeWhile(predicate)
  local result = {}
  local length = self:length()
  for j = 1, length do
    local elem = self[j]
    if not predicate(elem) then break end
    result[j] = elem
  end
  return collection.new(result)
end

function collection.mt:zip(that)
  local result = {}
  for j = 1, math.min(self:length(), that:length()) do
    result[j] = {self.values[j], that.values[j]}
  end
  return collection.new(result)
end

function collection.mt:zipWithIndex()
  return self:zip(self:indices())
end

function collection.mt:zipWithTail()
  return self:zip(self:tail())
end

-- Returns a collection with indices ranging between bounds (inclusive)
function collection.range(from, to)
  return collection.empty:padTo(0, to):indices():filter(function(index)
    return index >= from
  end)
end

function collection.new(elements)
  local col = {}
  col.values = elements
  setmetatable(col, collection.mt)
  return col
end

collection.empty = collection.new({})

function collection.single(x)
  return collection.new({x})
end

function collection.charsFromString(str)
  local chars = {}
  local length = #str
  for j = 1, length do
    chars[j] = str:sub(j, j)
  end
  return collection.new(chars)
end

Puppet.collection = collection


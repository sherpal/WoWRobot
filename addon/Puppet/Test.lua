-- Runs all tests suites when the file is loaded.

local collection = Puppet.collection

local allTests = {}

local function assertEquals(obtained, expected, clue)
  if obtained ~= expected then
    print(clue or "Obtained and expected where not equals.")
    print("Obtained: " .. tostring(obtained))
    print("Expected: " .. tostring(expected))

    error("Assertion failed, see above.")
  end
  
end

local function test(description, effect)

  allTests[#allTests+1] = function()
    print("Running test " .. description)
    effect()
  end
  
end

test("tostring and toString are the same", function()
  local col = collection.new({1,2,3,4})
  assertEquals(col:toString(), tostring(col))
end)

-- collection tests
test("Filter on even numbers return only even numbers", function()
  local col = collection.range(1, 10)
  local filtered = col:filter(function(elem) return elem % 2 == 0 end)
  assertEquals(filtered, collection.new({2,4,6,8,10}))
end)


test("Forall is true for empty collection", function()
  assertEquals(collection.empty:forall(function() return false end), true)
end)

test("This should fail", function()
  assertEquals(2, 3, "this was a test!")
end)


-- Runs all test cases
for _, fn in ipairs(allTests) do
  fn()
end


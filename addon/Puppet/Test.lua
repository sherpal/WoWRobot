-- Runs all tests suites when the file is loaded.

local collection = Puppet.collection
local json = Puppet.JsonSerializer
local base64 = Puppet.base64

local function pack(...)
  return { n = select("#", ...), ... }
end

local allSuites = {}

local function suite(name, ...)

  local tests = {}
  local rawTests = pack(...)
  for j = 1, rawTests.n do
    tests[#tests+1] = rawTests[j]
  end

  local s = {}
  s.tests = collection.new(tests)
  s.name = name
  function s:run()
    print("Test suite " .. self.name)
    self.tests:foreach(function(t) t.effect() end)
  end

  allSuites[#allSuites+1] = s
end

local function assertEquals(obtained, expected, clue)
  if obtained ~= expected then
    print(clue or "Obtained and expected where not equals.")
    print("Obtained: " .. tostring(obtained))
    print("Expected: " .. tostring(expected))

    error("Assertion failed, see above.")
  end
end

local function test(description, effect)
  local t = {}
  t.description = description
  t.effect = function()
    print("Running test " .. description)
    effect()
  end
  return t
end

local _1234 = collection.new({1, 2, 3, 4})

suite("Collection API",
  test("Collection instances are serializable", function ()
    assert(_1234.isSerializable, "Collection was not seriazable")
  end),

  test("Collection json serializes to array notation", function ()
    assertEquals(_1234:toJson(), _1234:mkString("[", ",", "]"))
  end),

  test("tostring and toString are the same", function()
    assertEquals(_1234:toString(), tostring(_1234))
  end),

  test("Accessing element by index return that element", function()
    for j = 1, _1234:length() do
      assertEquals(_1234[j], j)
    end
  end),

  test("Mk String on empty list is before and after", function()
    local empty = collection.empty

    assertEquals(empty:mkString(), "")
    assertEquals(empty:mkString("[", ", ", "]"), "[]")
  end),

  test("Mk String on _1234 works", function()
    assertEquals(_1234:mkString(", "), "1, 2, 3, 4")
    assertEquals(_1234:mkString(), "1234")
    assertEquals(_1234:mkString("[", ",", "]"), "[1,2,3,4]")
  end),

  test("Filter on even numbers return only even numbers", function()
    local col = collection.range(1, 10)
    local filtered = col:filter(function(elem) return elem % 2 == 0 end)
    assertEquals(filtered, collection.new({2,4,6,8,10}))
  end),

  test("Forall is true for empty collection", function()
    assertEquals(collection.empty:forall(function() return false end), true)
  end),

  test("Exists is false of empty collection", function ()
    assertEquals(collection.empty:exists(function() return true end), false)
  end),

  test("Sum works on range", function()
    for j = 1, 10 do
      assertEquals(
        collection.range(1, j):sum(),
        j * (j + 1) / 2,
        "Pascal triangle not satisfied!"
      )

      assertEquals(
        collection.range(1, j):sum(j),
        j * (j + 1) / 2 + j
      )
    end
  end)
)


suite("Json Serialization",
  test("Encoding a number or a boolean should be tostring", function()
    assertEquals(json.toJson(3), "3")
    assertEquals(json.toJson(true), "true")
  end),

  test("Encoding a string should add quotes", function ()
    assertEquals(json.toJson("coucou"), "\"coucou\"")
  end),

  test("Encoding flat object", function()
    local element = {
      hello = 3,
      stuff = "coucou",
      notUsed = 5
    }
    json.makeJsonSerializable(element, collection.new({"hello", "stuff"}))

    assertEquals(element:toJson(), "{\"hello\":3,\"stuff\":\"coucou\"}")
  end),

  test("Encoding nested object", function()
    local element = {
      hello = 3,
      notUsed = "hello",
      stuff = true,
      foo = {
        bar = 8,
        babar = "hey, you"
      }
    }
    json.makeJsonSerializable(element, collection.new({"hello", "stuff", "foo"}))
    json.makeJsonSerializable(element.foo, collection.new({"babar", "bar"}))

    assertEquals(
      json.toJson(element),
      "{\"hello\":3,\"stuff\":true,\"foo\":{\"babar\":\"hey, you\",\"bar\":8}}"
    )
  end),

  test("Encoding with a collection", function()
    local element = json.makeJsonSerializable({
      hello = 3,
      elements = collection.new({3, true, false, "hi"})
    }, collection.new({"hello", "elements"}))
    
    assertEquals(
      json.toJson(element),
      "{\"hello\":3,\"elements\":[3,true,false,\"hi\"]}"
    )
  end)
)


local function base64Roundtrip(str) return base64.decode(base64.encode(str)) end

suite("base64",
  test("Encoding and decoding string is identity", function()
    local value = "hello, friends! 64"
    assertEquals(
      base64Roundtrip(value),
      value
    )
  end),

  test("Encoding and decoding a Json encoded object", function()
    local element = json.makeJsonSerializable(
      {
        hello = 3,
        notUsed = "hello",
        stuff = true,
        foo = json.makeJsonSerializable({
          bar = 8,
          babar = "hey, you"
        }, collection.new({"bar", "babar"}))
      },
      collection.new({"hello", "stuff", "foo"})
    )

    assertEquals(
      base64Roundtrip(element:toJson()),
      element:toJson()
    )
  end)

)


-- Runs all test suites
collection.new(allSuites):foreach(function(s) s:run() end)

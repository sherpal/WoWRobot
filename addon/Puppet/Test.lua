-- Runs all tests suites when the file is loaded.

local collection = Puppet.collection
local json = Puppet.JsonSerializer
local base64 = Puppet.base64
local identity = Puppet.identity
local LIO = Puppet.LIO

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

  local s = {
    tests = collection.new(tests),
    name = name
  }
  function s:run()
    print("Test suite " .. self.name)
    self.tests:foreach(function(t) t.effect() end)
  end

  allSuites[#allSuites+1] = s
end

local function assertNil(element, clue)
  if type(element) ~= "nil" then
    error(clue or ("Element was supposed to be nil but got: " .. tostring(element)))
  end
end

local function assertEquals(obtained, expected, clue)
  if type(expected) == "nil" and type(clue) == "nil" then
    error("Expected can't be nil. Use 'assertNil' instead.")
  end

  if obtained ~= expected then
    print(clue or "Obtained and expected where not equals.")
    print("Obtained: " .. tostring(obtained))
    print("Expected: " .. tostring(expected))

    error("Assertion failed, see above.")
  end
end

local function test(description, effect)
  return {
    description = description,
    effect = function()
      print("Running test " .. description)
      effect()
    end
  }
end

local function lioTest(description, effect)
  return {
    description = description,
    effect = function()
      local l = LIO.clock.sleep(0):thenRun(LIO.console.print(description)):thenRun(effect)
      LIO.runToFuture(l)
    end
  }
end

local _1234 = collection.range(1, 4)

suite("Meta",
  test("Assert nil works", function()
    assertNil(nil, "Nil was correctly identity")
    assertNil(nil)

  end)
)

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

  test("Pad left to", function ()
    assertEquals(
      _1234:padLeftTo(0, 6),
      collection.new({0, 0, 1, 2, 3, 4})
    )
    assertEquals(_1234:padLeftTo(0, 3), _1234)
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
  end),

  test("take method", function()
    assertEquals(_1234:take(2), collection.new({1, 2}))
    assertEquals(_1234:take(5), identity(_1234))
  end),

  test("takeWhile method", function()
    local col = collection.new({true, true, false, true, true})
    assertEquals(col:takeWhile(identity), col:take(2))
  end),

  test("Grouped method", function()
    local elements = collection.range(1, 1000)

    assertEquals(elements:grouped(10):length(), 100)
    assert(elements:grouped(10):forall(function(group) return group:length() == 10 end))
    assertEquals(elements:grouped(501):length(), 2)
    assertEquals(elements:grouped(501)[1]:length(), 501)
    assertEquals(elements:grouped(501)[2]:length(), 499)

    assertEquals(
      elements:grouped(11):flatten(),
      identity(elements),
      "grouped then flatten should be identity function"
    )
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
  end),

  test("Auto encoding with nested and collection", function ()
    local element = json.makeJsonSerializableAuto({
      hello = 3,
      elements = collection.range(1, 3),
      noThere = function() end,
      nested = {
        hey = 4
      }
    })

    assertEquals(element:keysToJsonEncode():length(), 3)
    assertEquals(element.nested:keysToJsonEncode():length(), 1)
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

suite("Config",
  test("Size of squares is posivite", function()
    assert(Puppet.config.squaresPixelSize > 0)
  end)
)

suite("Utils",
  test("Transformation to base works", function()
    local n = 10
    local baseN = Puppet.utils.baseN
    assertEquals(baseN(n, 2), collection.new({1, 0, 1, 0}))
    assertEquals(baseN(n, 3), collection.new({1, 0,  1}))
    assertEquals(baseN(n, 4), collection.new({2, 2}))
    assertEquals(baseN(n, 5), collection.new({2, 0}))
  end)
)

suite("LIO",
  lioTest(
    "Testing with a sleep",
    LIO.clock.sleep(1):thenRun(LIO.fromFunction(function()
      assertEquals(1, 1, "bleh")
    end))
  ),

  lioTest("Forking must parallelize stuff",
    LIO.clock.sleep(3):as(5):fork():flatMap(function(sleepFiber)
      return LIO.fromFunction(function()
        assertNil(sleepFiber.result, "Fiber result was supposed to be nil at this point")
      end):as(5):flatMap(function(x)
        return sleepFiber:join():map(function(y)
          return x + y
        end)
      end)
    end):flatMap(function(result)
      return LIO.fromFunction(function()
        assertEquals(result, 10)
      end)
    end)
  )
)


-- Runs all test suites
collection.new(allSuites):foreach(function(s) s:run() end)

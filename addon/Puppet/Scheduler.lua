
local scheduler = {}

--------------------------------------------------------------------------------
--- Scheduler capability ---
----------------------------

local mainFrame = CreateFrame("Frame")
mainFrame:Hide()

-- Timers APIs

local timers = {}

mainFrame.timers = timers
mainFrame.timersToRemove = {}

function mainFrame:AddNewTimer(duration, endProcedure, Arguments, name)
  if type(name) == "number" then error("Timer name can not be a number!") end
  self.timers = self.timers or {}
  if name and self.timers[name] then self:AbortProcedure(name) end
  self.timers[#self.timers + 1] = {duration, endProcedure, Arguments}
  self.timers[#self.timers].id = #self.timers
  if name then self.timers[name] = self.timers[#self.timers] end
  self.timers[#self.timers].name = name
  self:Show()
end

function mainFrame:AbortProcedure(name)
  if not name then return nil end
  if not self.timers then return nil end
  if not self.timers[name] then return nil end
  self.timersToRemove[#self.timersToRemove+1] = name
end

function scheduler.setTimeout(durationInSeconds, callback, args, name)
  mainFrame:AddNewTimer(durationInSeconds, callback, args, name)
end

function scheduler.setInterval(initialDelayInSeconds, intervalInSeconds, callback, name)
  local endCallback = function()
    callback()
    scheduler.setInterval(intervalInSeconds, intervalInSeconds, callback, name)
  end

  scheduler.setTimeout(0, function() -- need to do this to avoid the abort procedure at beginning of next cycle
    scheduler.setTimeout(initialDelayInSeconds, endCallback, nil, name)
  end)
end

function scheduler.abort(name)
  mainFrame:AbortProcedure(name)
end

local function checkTimers(self, elapsed)
  for _, name in ipairs(self.timersToRemove) do
    table.remove(self.timers, self.timers[name].id)
    self.timers[name] = nil
  end

  local toRemove = {}
  for j = 1, #self.timers do
    self.timers[j][1] = self.timers[j][1] - elapsed
    -- player most likely returned windows (or looked at map). We add 5
    -- seconds to avoid crash
    if self.timers[j][1] < -2 then
      self.timers[j][1] = 5
    end
    if self.timers[j][1] <= 0 and not self.timers[j].done then
      self.timers[j].done = true
      self.timers[j][2](self.timers[j][3])
      if self.timers[j].name then
        self.timers[self.timers[j].name] = nil
      end
      -- table.remove(self.timers, j)
      table.insert(toRemove, j)
      if #self.timers == 0 then self:HideIfDoNothing() end
    end
  end
  for j = #toRemove, 1, -1 do
    table.remove(self.timers, j)
  end
end


-- Future API

mainFrame.futures = Puppet.collection.empty

local function checkFutures(self)
  local finished, stillRunning = self.futures:partition(function(future) 
    return future.completed end
  )
  mainFrame.futures = stillRunning:filterNot(function(element) return element.cancelled end)
  if finished:length() > 0 then
    print("Number of finished futures: " .. finished:length())
  end
  finished:foreach(function(f)
    f.callbacks:foreach(function(callback)
      callback(f.value)
      f.takenCareOf = true
    end)
  end)
  self:HideIfDoNothing()
end

-- todo: think about this
local future = {}
scheduler.Future = future

future.mt = {}
future.mt.__index = future.mt

function future.mt:as(value)
  return self:map(function() return value end)
end

function future.mt:cancel()
  self.cancelled = true
end

function future.mt:complete(value)
  self.completed = true
  self.value = value
end

function future.mt:flatMap(f)
  local ff = future.new()
  self:onComplete(function(value)
    f(value):onComplete(function(nextValue)
      ff:complete(nextValue)
    end)
  end)
  return ff
end

function future.mt:map(f)
  return self:flatMap(function(value)
    return future.successfull(f(value))
  end)
end

function future.mt:onComplete(callback)
  if self.takenCareOf then
    callback(self.value)
  else
    self.callbacks = self.callbacks:append(callback)
  end
end

function future.mt:thenWait(numberOfSeconds)
  return self:flatMap(function(value)
    return future.sleep(numberOfSeconds):as(value)
  end)
end

function future.new(callbacks)
  local f = {
    callbacks = callbacks or Puppet.collection.empty,
    completed = false,
    cancelled = false
  }
  mainFrame.futures = mainFrame.futures:append(f)
  mainFrame:Show()
  setmetatable(f, future.mt)
  return f
end

function future.successfull(value)
  local f = future.new()
  f:complete(value)
  return f
end

function future.sleep(numberOfSeconds)
  local f = future.new()
  scheduler.setTimeout(numberOfSeconds, function()
    f:complete()
  end)
  return f
end


-- LIO api

local lio = {}

lio.mt = {}
lio.mt.__index = lio.mt

function lio.mt:run(callback)
  print(callback)
  error("NotImplemented")
end

function lio.cons()
  return setmetatable({}, lio.mt)
end

function lio.async(callbackToBeCalled)
  local theLio = lio.cons()
  function theLio:run(givenCallback)
    -- givenCallback is A => Unit
    -- callbackToBeCalled is (A => Unit) => Unit
    callbackToBeCalled(givenCallback)
  end
  return theLio
end

-- Creates a LIO[B] given a Lio[A] and a f: A => LIO[B]
function lio.flatMap(theLio, f)
  local flatMappedLio = lio.cons()
  function flatMappedLio:run(callback)
    theLio:run(function(a)
      f(a):run(callback)
    end)
  end
  return flatMappedLio
end

function lio.fromFunction(f)
  return lio.unit(nil):map(function() return f() end)
end

-- Given a theFuture: () => Future[A], creates a
-- LIO[A] that will run when the Future completes
function lio.fromFuture(theFuture)
  if type(theFuture) ~= "function" then
    error("theFuture argument must be a function", 2)
  end
  return lio.async(function(callback) -- callback is A => Unit
    theFuture():onComplete(callback)
  end)
end

-- Execute the effect asynchronously, and return a future which will be
-- completed with the value of the specified lio effect.
function lio.runToFuture(theLio)
  local f = future.new()
  theLio:run(function(a)
    f:complete(a)
  end)
  return f
end

-- Creates a LIO[A], where A is the type of value
function lio.unit(value)
  local effect = lio.cons()
  function effect:run(callback)
    callback(value)
  end
  return effect
end

-- Executes this effect, then replaces its output with value
function lio.mt:as(value)
  return self:map(function() return value end)
end

-- Should I comment flatMap?
function lio.mt:flatMap(f)
  return lio.flatMap(self, f)
end

-- Should I comment map?
function lio.mt:map(f)
  return self:flatMap(function(a) return lio.unit(f(a)) end)
end

-- Applies this effect continually for the rest of times.
function lio.mt:repeatForever()
  -- it is required to wait 0s, otherwise a synchronous effect will produce a stack overflow
  return self:thenWait(0):flatMap(function() return self:repeatForever() end)
end

-- Run this effect then that effect, returning the result of that effect
function lio.mt:thenRun(that)
  return self:flatMap(function() return that end)
end

-- Applies this effect, then wait the specified time before completing.
function lio.mt:thenWait(durationInSeconds)
  return self:flatMap(function(a)
    return lio.clock.sleep(durationInSeconds):as(a)
  end)
end

lio.clock = {
  sleep = function(durationInSeconds)
    return lio.fromFuture(function() return future.sleep(durationInSeconds) end)
  end
}

lio.console = {
  print = function(contents)
    return lio.fromFunction(function() print(contents) end)
  end
}


mainFrame:SetScript("OnUpdate", function(self, elapsed)
  checkTimers(self, elapsed)
  checkFutures(self)
end)

function mainFrame:HideIfDoNothing()
  if #self.timers == 0 and self.futures:isEmpty() then
    self:Hide()
  end
end


Puppet.Scheduler = scheduler
Puppet.Future = future
Puppet.LIO = lio
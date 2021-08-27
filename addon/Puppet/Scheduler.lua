
local scheduler = {}

--------------------------------------------------------------------------------
--- Scheduler capability ---
----------------------------

local mainFrame = CreateFrame("Frame")
mainFrame:Hide()

local timers = {}

mainFrame.timers = timers

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
  table.remove(self.timers, self.timers[name].id)
  self.timers[name] = nil
end


mainFrame:SetScript("OnUpdate", function(self, elapsed)
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
      if #self.timers == 0 then self:Hide() end
    end
  end
  for j = #toRemove, 1, -1 do
    table.remove(self.timers, j)
  end
end)

function scheduler.setTimeout(durationInSeconds, callback, args, name)
  mainFrame:AddNewTimer(durationInSeconds, callback, args, name)
end

function scheduler.abort(name)
  mainFrame:AbortProcedure(name)
end

-- todo: think about this
local future = {}
scheduler.Future = future

future.mt = {}
future.mt.__index = future.mt

function future.mt:onComplete(callback)
  self.callbacks[#self.callbacks+1] = callback
end

function future.mt:flatMap(f)

end

function future.new(callbacks)
  local f = {
    callbacks = callbacks
  }

  setmetatable(future.mt, f)
  return f
end


Puppet.Scheduler = scheduler


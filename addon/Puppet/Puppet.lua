Puppet = {}

-- this is not necessary but VSC complaints that WorldFrame does not exist,
-- hence it is annoying to use it like this
Puppet.WorldFrame = WorldFrame
-- wow has an old version of Lua where table.unpack is not yet there, but VSC complaints
-- that global unpack is deprecated, hence the need to fill it here so that we avoid
-- unnecessary warnings everywhere.
table.unpack = unpack

-- /console scriptErrors 1

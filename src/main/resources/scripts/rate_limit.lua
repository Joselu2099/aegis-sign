local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = 1

local bucket = redis.call('hmget', key, 'tokens', 'lastRefillTimestamp')
local tokens = tonumber(bucket[1])
local lastRefillTimestamp = tonumber(bucket[2])

if tokens == nil then
    tokens = capacity
    lastRefillTimestamp = now
end

local elapsedTime = math.max(0, now - lastRefillTimestamp)
local tokensToAdd = elapsedTime * refillRate
tokens = math.min(capacity, tokens + tokensToAdd)

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('hmset', key, 'tokens', tokens, 'lastRefillTimestamp', now)
local expireTime = 3600
if refillRate > 0 then
    expireTime = math.ceil(capacity / refillRate) + 10
end
redis.call('expire', key, expireTime)

return allowed

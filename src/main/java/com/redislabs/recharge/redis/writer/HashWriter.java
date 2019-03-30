package com.redislabs.recharge.redis.writer;

import java.util.Map;

import com.redislabs.lettusearch.RediSearchAsyncCommands;
import com.redislabs.recharge.redis.RedisType;

import io.lettuce.core.RedisFuture;

public class HashWriter extends AbstractSingleRedisWriter {

	@Override
	protected RedisFuture<?> writeSingle(String key, Map<String, Object> record,
			RediSearchAsyncCommands<String, String> commands) {
		Map<String, String> stringRecord = toStringMap(record);
		return commands.hmset(key, stringRecord);
	}

	@Override
	public RedisType getRedisType() {
		return RedisType.Hash;
	}
}
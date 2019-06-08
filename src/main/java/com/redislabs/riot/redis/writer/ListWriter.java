package com.redislabs.riot.redis.writer;

import java.util.Map;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.Setter;
import redis.clients.jedis.Pipeline;

@Setter
public class ListWriter extends AbstractCollectionRedisItemWriter {

	@Override
	protected void write(Pipeline pipeline, String key, String member, Map<String, Object> item) {
		pipeline.lpush(key, member);
	}

	@Override
	protected RedisFuture<?> write(RedisAsyncCommands<String, String> commands, String key, String member,
			Map<String, Object> item) {
		return commands.lpush(key, member);
	}

}
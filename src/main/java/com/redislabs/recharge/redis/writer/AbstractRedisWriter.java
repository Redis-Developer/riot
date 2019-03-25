
package com.redislabs.recharge.redis.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import com.redislabs.lettusearch.RediSearchAsyncCommands;
import com.redislabs.lettusearch.StatefulRediSearchConnection;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRedisWriter extends AbstractItemStreamItemWriter<Map<String, Object>> {

	public static final String KEY_SEPARATOR = ":";

	protected ConversionService converter = new DefaultConversionService();
	protected GenericObjectPool<StatefulRediSearchConnection<String, String>> pool;

	public void setPool(GenericObjectPool<StatefulRediSearchConnection<String, String>> pool) {
		this.pool = pool;
	}

	protected String getValues(Map<String, Object> record, String[] fields) {
		if (fields.length == 0) {
			return null;
		}
		String[] values = new String[fields.length];
		Arrays.setAll(values, index -> converter.convert(record.get(fields[index]), String.class));
		return join(values);
	}

	protected String join(String... values) {
		return String.join(KEY_SEPARATOR, values);
	}

	protected Map<String, String> toStringMap(Map<String, Object> record) {
		Map<String, String> stringMap = new HashMap<String, String>();
		for (String key : record.keySet()) {
			Object value = record.get(key);
			stringMap.put(key, converter.convert(value, String.class));
		}
		return stringMap;
	}

	@Override
	public void write(List<? extends Map<String, Object>> records) throws Exception {
		List<RedisFuture<?>> futures = new ArrayList<>();
		if (pool.isClosed()) {
			return;
		}
		StatefulRediSearchConnection<String, String> connection = pool.borrowObject();
		try {
			RediSearchAsyncCommands<String, String> commands = connection.async();
			commands.setAutoFlushCommands(false);
			for (Map<String, Object> record : records) {
				RedisFuture<?> future = write(record, commands);
				if (future != null) {
					futures.add(future);
				}
			}
			commands.flushCommands();
			try {
				boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS,
						futures.toArray(new RedisFuture[futures.size()]));
				if (result) {
					log.debug("Wrote {} records", records.size());
				} else {
					log.warn("Could not write {} records", records.size());
					for (RedisFuture<?> future : futures) {
						if (future.getError() != null) {
							log.error(future.getError());
						}
					}
				}
			} catch (RedisCommandExecutionException e) {
				log.error("Could not execute commands", e);
			}
		} finally {
			pool.returnObject(connection);
		}
	}

	protected abstract RedisFuture<?> write(Map<String, Object> record,
			RediSearchAsyncCommands<String, String> commands);

}

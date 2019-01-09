package net.inetalliance.web;

import net.inetalliance.log.Log;
import net.inetalliance.potion.cache.RedisJsonCache;
import net.inetalliance.types.json.Json;
import net.inetalliance.types.json.JsonMap;
import net.inetalliance.util.security.auth.Authorized;
import net.inetalliance.web.errors.InternalServerError;
import net.inetalliance.web.errors.NotFoundError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public abstract class LongTask
	extends JsonProcessor {
	private final ExecutorService executor;

	private final Map<Integer, Task> tasks;

	private int nextId;

	public LongTask() {
		tasks = new TreeMap<>();
		nextId = 1;
		executor = Executors.newCachedThreadPool(r -> {
			final Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
	}

	protected RedisJsonCache getCache() {
		return null;
	}

	protected void cache(final RedisJsonCache cache, final String key, final JsonMap response) {

	}

	protected String getCacheKey(final HttpServletRequest request, final Authorized authorized) {
		return null;
	}

	@Override
	protected Json $(final HttpMethod method, final HttpServletRequest request,
	                 final HttpServletResponse response, final Authorized authorized) {

		final RedisJsonCache cache = getCache();
		final String cacheKey;
		if (cache != null) {
			cacheKey = getCacheKey(request, authorized);
			log.debug("cache key: %s", cacheKey);
			final Json cached = cache.getMap(cacheKey);
			if (cached != null) {
				return cached;
			}
		} else {
			cacheKey = null;
		}

		final Integer id = getParam(request, Integer.class, "id");
		if (id == null) {
			final Task task = newTask(request, authorized, nextId++);
			tasks.put(task.id, task);
			executor.submit((Runnable) task);
			return task.toJson();
		} else {
			final Task task = tasks.get(id);
			if (task == null) {
				throw new NotFoundError();
			} else {
				if (task.t != null) {
					throw new InternalServerError(task.t);
				} else if (task.response != null) {
					if (cache != null) {
						cache(cache, cacheKey, task.response);
					}
					return task.response;
				} else {
					return task.toJson();
				}
			}
		}
	}

	protected abstract Task newTask(final HttpServletRequest request, final Authorized authorized, final int id);

	protected abstract static class Task
		implements Supplier<JsonMap>, Runnable {
		private final int id;
		private final int maxValue;
		public String label;
		public int value;
		protected Throwable t;
		protected JsonMap response;
		private final transient JsonMap json;

		protected Task(final int id, final int maxValue) {
			this.id = id;
			this.maxValue = maxValue;
			json = new JsonMap();
		}

		public Json toJson() {
			json.put("id", id);
			json.put("running", true);
			json.put("maxValue", maxValue);
			json.put("value", value);
			json.put("label", label);
			return json;
		}

		@Override
		public final void run() {
			try {
				this.response = get();
			} catch (Throwable t) {
				this.t = t;
			}
		}
	}

	private static transient final Log log = Log.getInstance(LongTask.class);
}

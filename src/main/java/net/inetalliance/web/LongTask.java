package net.inetalliance.web;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.inetalliance.types.json.Json;
import net.inetalliance.types.json.JsonMap;
import net.inetalliance.util.security.auth.Authorized;
import net.inetalliance.web.errors.InternalServerError;
import net.inetalliance.web.errors.NotFoundError;

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

  @Override
  protected Json $(final HttpMethod method, final HttpServletRequest request,
      final HttpServletResponse response,
      final Authorized authorized) {

    final Integer id = getParam(request, "id", s -> s.map(Integer::valueOf).orElse(null));
    if (id == null) {
      final Task task = newTask(request, authorized, nextId++);
      tasks.put(task.id, task);
      executor.submit(task);
      return task.toJson();
    } else {
      final Task task = tasks.get(id);
      if (task == null) {
        throw new NotFoundError();
      } else {
        if (task.t != null) {
          throw new InternalServerError(task.t);
        } else if (task.response != null) {
          return task.response;
        } else {
          return task.toJson();
        }
      }
    }
  }

  abstract Task newTask(final HttpServletRequest request, final Authorized authorized,
      final int id);

  protected abstract static class Task
      implements Supplier<JsonMap>, Runnable {

    private final int id;
    private final int maxValue;
    private final transient JsonMap json;
    protected JsonMap response;
    String label;
    int value;
    Throwable t;

    protected Task(final int id, final int maxValue) {
      this.id = id;
      this.maxValue = maxValue;
      json = new JsonMap();
    }

    Json toJson() {
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

}

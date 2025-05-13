package net.inetalliance.web.errors;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.inetalliance.types.json.JsonList;
import net.inetalliance.types.json.JsonMap;
import net.inetalliance.types.json.Pretty;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.SecureProcessor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

public class InternalServerError
    extends HttpError {

  private static final Pattern dot = Pattern.compile("\\.");

  public InternalServerError(final String message, final Throwable cause) {
    super(message, cause);
  }

  public InternalServerError(final Throwable cause) {
    super(cause);
  }

  public static void $(final HttpServletResponse response, final Throwable t)
      throws IOException {
    final JsonMap json = new JsonMap();
    json.put("type", t.getClass().getName());
    json.put("message", t.getMessage());
    final StackTraceElement[] stack = t.getStackTrace();
    final JsonList stackJson = new JsonList(stack.length);
    for (final StackTraceElement frame : stack) {
      final JsonMap frameJson = new JsonMap();
      frameJson.put("class", frame.getClassName());
      final String[] split = dot.split(frame.getClassName());
      frameJson.put("simple", split[split.length - 1]);
      frameJson.put("method", frame.getMethodName());
      frameJson.put("line", Integer.toString(frame.getLineNumber()));
      stackJson.add(frameJson);
    }
    json.put("stack", stackJson);
    response.setStatus(SC_INTERNAL_SERVER_ERROR);
    response.setContentType(SecureProcessor.jsonContentType);
    final PrintWriter writer = response.getWriter();
    Pretty.$(json, writer);
    writer.flush();
  }

  @Override
  public void $(final HttpMethod method, final HttpServletRequest request,
      final HttpServletResponse response)
      throws IOException {
    $(response, getCause());
  }

  @Override
  public int getCode() {
    return SC_INTERNAL_SERVER_ERROR;
  }
}

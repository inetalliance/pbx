package net.inetalliance.web;

import static net.inetalliance.funky.StringFun.isNotEmpty;
import static net.inetalliance.types.www.ContentType.JAVA_SERIALIZED_OBJECT;
import static net.inetalliance.web.HttpMethod.DELETE;
import static net.inetalliance.web.HttpMethod.GET;
import static net.inetalliance.web.HttpMethod.HEAD;
import static net.inetalliance.web.HttpMethod.OPTIONS;
import static net.inetalliance.web.HttpMethod.POST;
import static net.inetalliance.web.HttpMethod.PUT;
import static net.inetalliance.web.HttpMethod.TRACE;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.inetalliance.daemonic.RuntimeKeeper;
import net.inetalliance.funky.Funky;
import net.inetalliance.log.Log;
import net.inetalliance.potion.annotations.Permitted;
import net.inetalliance.util.security.auth.Authorized;
import net.inetalliance.web.errors.ForbiddenError;
import net.inetalliance.web.errors.HttpError;
import net.inetalliance.web.errors.InternalServerError;

public abstract class Processor
    extends HttpServlet {

  public static final String jsonContentType = JAVA_SERIALIZED_OBJECT.toString();
  private static final transient Log log = Log.getInstance(Processor.class);

  private static String toString(final HttpServletRequest request) {
    final StringBuilder out = new StringBuilder();
    out.append("\n***** REQUEST: ").append(request.getRequestURI()).append(" *****\n");
    final Map<String, String[]> parameters = getParameters(request);
    if (!parameters.isEmpty()) {
      out.append("  Parameters:\n");
    }
    for (final Map.Entry<String, String[]> parameter : parameters.entrySet()) {
      for (final String value : parameter.getValue()) {
        out.append("  -- ").append(parameter.getKey()).append(": \"").append(value).append("\"\n");
      }
    }
    out.append("**********");
    return out.toString();
  }

  private static Map<String, String[]> getParameters(final HttpServletRequest request) {
    return request.getParameterMap();
  }

  static <T> T getParam(final HttpServletRequest request, final String name,
      Function<Optional<String>, T> parse) {
    return parse.apply(Funky.of(request.getParameter(name)));
  }

  protected static <E extends Enum<E>> E getParam(final HttpServletRequest request,
      final String name,
      final Class<E> enumType) {
    return getParam(request, name,
        s -> s.map(String::toUpperCase).map(n -> Enum.valueOf(enumType, n)).orElse(null));
  }

  protected static String getInitParameter(final ServletConfig config, final String key) {
    if (RuntimeKeeper.isDevelopment()) {
      final String value = config.getInitParameter(String.format("dev-%s", key));
      if (isNotEmpty(value)) {
        return value;
      }
    }
    return config.getInitParameter(key);
  }

  @Override
  protected final void doDelete(final HttpServletRequest request,
      final HttpServletResponse response)
      throws IOException {
    doAll(DELETE, request, response);
  }

  protected void doAll(final HttpMethod method, final HttpServletRequest request,
      final HttpServletResponse response)
      throws IOException {
    try {
      final Permitted permitted = getClass().getAnnotation(Permitted.class);
      if (permitted != null) {
        final Authorized authorized = Auth.getAuthorized(request);
        for (final String role : permitted.value()) {
          if (isNotEmpty(role) && !authorized.isAuthorized(role)) {
            throw new ForbiddenError();
          }
        }
      }
      $(method, request, response);
    } catch (HttpError e) {
      log.error(toString(request), e);
      e.$(method, request, response);
    } catch (Throwable t) {
      log.error(toString(request), t);
      InternalServerError.$(response, t);
    }
  }

  public abstract void $(final HttpMethod method, final HttpServletRequest request,
      final HttpServletResponse response)
      throws Throwable;

  @Override
  protected final void doGet(final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    doAll(GET, request, response);
  }

  @Override
  protected final void doHead(final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    doAll(HEAD, request, response);
  }

  @Override
  protected final void doOptions(final HttpServletRequest request,
      final HttpServletResponse response)
      throws IOException {
    doAll(OPTIONS, request, response);
  }

  @Override
  protected final void doPost(final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    doAll(POST, request, response);
  }

  @Override
  protected final void doPut(final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    doAll(PUT, request, response);
  }

  @Override
  protected final void doTrace(final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    doAll(TRACE, request, response);
  }
}

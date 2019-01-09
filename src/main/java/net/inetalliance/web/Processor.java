package net.inetalliance.web;

import net.inetalliance.daemonic.RuntimeKeeper;
import net.inetalliance.log.Log;
import net.inetalliance.potion.annotations.Permitted;
import net.inetalliance.util.security.auth.Authorized;
import net.inetalliance.web.errors.ForbiddenError;
import net.inetalliance.web.errors.HttpError;
import net.inetalliance.web.errors.InternalServerError;
import org.joda.time.DateMidnight;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static net.inetalliance.funky.ClassFun.convert;
import static net.inetalliance.funky.StringFun.isEmpty;
import static net.inetalliance.funky.StringFun.isNotEmpty;
import static net.inetalliance.types.www.ContentType.JAVA_SERIALIZED_OBJECT;
import static net.inetalliance.web.HttpMethod.*;

public abstract class Processor extends HttpServlet {
	public static final String jsonContentType = JAVA_SERIALIZED_OBJECT.toString();
	private static final transient Log log = Log.getInstance(Processor.class);

	protected static String toString(final HttpServletRequest request) {
		final StringBuilder out = new StringBuilder();
		out.append("\n***** REQUEST: ").append(request.getRequestURI()).append(" *****\n");
		final Map<String, String[]> parameters = getParameters(request);
		if (!parameters.isEmpty())
			out.append("  Parameters:\n");
		for (final Map.Entry<String, String[]> parameter : parameters.entrySet()) {
			for (final String value : parameter.getValue())
				out.append("  -- ").append(parameter.getKey()).append(": \"").append(value).append("\"\n");
		}
		out.append("**********");
		return out.toString();
	}

	public static Map<String, String[]> getParameters(final HttpServletRequest request) {
		return request.getParameterMap();
	}

	protected static <T> T getParam(final HttpServletRequest request, final Class<T> type, final String name) {
		try {
			return convert(type, request.getParameter(name));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected static <E extends Enum<E>> E getParam(final HttpServletRequest request, final String name,
	                                                final E defaultValue) {
		final String value = request.getParameter(name);
		return isEmpty(value) ? defaultValue : Enum.valueOf(defaultValue.getDeclaringClass(), value);
	}

	protected static DateMidnight getParam(final HttpServletRequest request, final String name,
	                                       final DateMidnight defaultValue) {
		final String s = request.getParameter(name);
		try {
			return isEmpty(s) ? defaultValue : convert(DateMidnight.class, s);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected static Integer getParam(final HttpServletRequest request, final String name, final Integer defaultValue) {
		final String s = request.getParameter(name);
		return isEmpty(s) ? defaultValue : new Integer(s);
	}

	protected static boolean getParam(final HttpServletRequest request, final String name, final boolean defaultValue) {
		final String s = request.getParameter(name);
		return isEmpty(s) ? defaultValue : "true".equals(s);
	}

	protected static Long getParam(final HttpServletRequest request, final String name, final Long defaultValue) {
		final String s = request.getParameter(name);
		return isEmpty(s) ? defaultValue : new Long(s);
	}

	protected static <T> List<T> getParamValues(final HttpServletRequest request, final Class<T> type,
	                                            final String name) {
		final String[] values = request.getParameterValues(name);

		if (values == null || values.length == 0)
			return Collections.emptyList();

		final List<T> list = new ArrayList<T>(values.length);
		for (String value : values) {
			if (isNotEmpty(value)) {
				try {
					list.add(convert(type, value));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return list;

	}

	public static String getCacheKey(final HttpServletRequest request, final Authorized authorized,
	                                 final String... parameters) {
		String cacheKey = (String) request.getAttribute("cacheKey");
		if (cacheKey == null) {
			cacheKey = Arrays.stream(parameters).map(parameter -> String.format("%s=%s", parameter,
				request.getParameter(parameter))).collect(Collectors.joining(","));
			cacheKey = authorized == null
				? cacheKey
				: String.format("%s-%s", cacheKey, authorized.getPhone());
			request.setAttribute("cacheKey", cacheKey);
		}
		return cacheKey;
	}

	public static String getInitParameter(final ServletConfig config, final String key) {
		if (RuntimeKeeper.isDevelopment()) {
			final String value = config.getInitParameter(String.format("dev-%s", key));
			if (isNotEmpty(value))
				return value;
		}
		return config.getInitParameter(key);
	}

	@Override
	protected final void doDelete(final HttpServletRequest request, final HttpServletResponse response)
		throws IOException {
		doAll(DELETE, request, response);
	}

	protected void doAll(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
		throws IOException {
		try {
			final Permitted permitted = getClass().getAnnotation(Permitted.class);
			if (permitted != null) {
				final Authorized authorized = Auth.getAuthorized(request);
				for (final String role : permitted.value()) {
					if (isNotEmpty(role) && !authorized.isAuthorized(role))
						throw new ForbiddenError();
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

	public abstract void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
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
	protected final void doOptions(final HttpServletRequest request, final HttpServletResponse response)
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

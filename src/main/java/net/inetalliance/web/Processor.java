package net.inetalliance.web;

import com.ameriglide.phenix.core.Log;
import com.ameriglide.phenix.core.Optionals;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.cli.Cli;
import net.inetalliance.potion.annotations.Permitted;
import net.inetalliance.web.errors.ForbiddenError;
import net.inetalliance.web.errors.HttpError;
import net.inetalliance.web.errors.InternalServerError;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.ameriglide.phenix.core.Strings.isNotEmpty;
import static net.inetalliance.types.www.ContentType.JAVA_SERIALIZED_OBJECT;
import static net.inetalliance.web.HttpMethod.*;

public abstract class Processor
        extends HttpServlet {

    public static final String jsonContentType = JAVA_SERIALIZED_OBJECT.toString();
    private static final Log log = new Log();

    private static String toString(final HttpServletRequest request) {
        val out = new StringBuilder();
        out.append("\n***** REQUEST: ").append(request.getRequestURI()).append(" *****\n");
        val parameters = getParameters(request);
        if (!parameters.isEmpty()) {
            out.append("  Parameters:\n");
        }
        for (val parameter : parameters.entrySet()) {
            for (val value : parameter.getValue()) {
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
        return parse.apply(Optionals.of(request.getParameter(name)));
    }

    protected static <E extends Enum<E>> E getParam(final HttpServletRequest request,
                                                    final String name,
                                                    final Class<E> enumType) {
        return getParam(request, name,
                s -> s.map(String::toUpperCase).map(n -> Enum.valueOf(enumType, n)).orElse(null));
    }

    protected static String getInitParameter(final ServletConfig config, final String key) {
        if (Cli.isDevelopment()) {
            val value = config.getInitParameter(String.format("dev-%s", key));
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
            val permitted = getClass().getAnnotation(Permitted.class);
            if (permitted != null) {
                val authorized = Auth.getAuthorized(request);
                for (val role : permitted.value()) {
                    if (isNotEmpty(role) && !authorized.isAuthorized(role)) {
                        throw new ForbiddenError();
                    }
                }
            }
            $(method, request, response);
        } catch (HttpError e) {
            log.error(() -> toString(request), e);
            e.$(method, request, response);
        } catch (Throwable t) {
            log.error(() -> toString(request), t);
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

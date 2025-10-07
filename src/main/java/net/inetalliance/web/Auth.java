package net.inetalliance.web;


import com.ameriglide.phenix.core.Log;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.types.json.JsonMap;
import net.inetalliance.types.json.Pretty;
import net.inetalliance.types.www.ContentType;
import net.inetalliance.util.security.auth.Authenticator;
import net.inetalliance.util.security.auth.Authorized;
import net.inetalliance.util.security.auth.Authorizer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.ameriglide.phenix.core.Strings.isNotEmpty;
import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;

public class Auth
        extends Processor {

    private static final Pattern logout = Pattern.compile("(.*)/logout");
    private static final Log log = new Log();
    public static Authenticator authenticator;
    public static Authorizer authorizer;
    public static String asset;

    public static boolean isAuthenticated(final HttpServletRequest request,
                                          final HttpServletResponse response)
            throws IOException {
        log.trace(() -> "checking if %s is authenticated".formatted(request.getSession().getId()));
        val authorized = getAuthorized(request);
        if (authorized != null) {
            log.trace(() -> "%s is authenticated".formatted(request.getSession().getId()));
            return true;
        }
        val cookies = request.getCookies();
        if (cookies != null) {
            for (val cookie : cookies) {
                if ("authToken".equals(cookie.getName())) {
                    log.trace(() -> "%s has an authToken".formatted(request.getSession().getId()));
                    val ticket = authenticator.login(cookie.getValue());
                    if (ticket == null || ticket.getToken() == null) {
                        break;
                    } else {
                        log.trace(() -> "%s has a valid authToken".formatted(request.getSession().getId()));
                        request.getSession().setAttribute("authorized", authorizer.bind(ticket));
                        return true;
                    }
                }
            }
        }
        log.trace(() -> "%s is not authenticated".formatted(request.getSession().getId()));
        response.sendError(SC_FORBIDDEN);
        return false;
    }

    public static Authorized getAuthorized(final HttpServletRequest request) {
        return (Authorized) request.getSession().getAttribute("authorized");
    }

    @Override
    public void $(final HttpMethod method, final HttpServletRequest request,
                  final HttpServletResponse response)
            throws Throwable {
        val matcher = logout.matcher(request.getRequestURI());
        if (matcher.matches()) {
            val session = request.getSession();
            log.debug(() -> "logging out %s".formatted(session.getId()));
            session.removeAttribute("authorized");
            session.invalidate();
            val cookie = new Cookie("authToken", "");
            cookie.setMaxAge(-1);
            response.addCookie(cookie);
            response.sendRedirect(matcher.group(1));
        } else  // login
        {
            val name = request.getParameter("username");
            val password = request.getParameter("password");
            val session = request.getSession();
            val ticket = authenticator.login(name, password, session.getId());
            synchronized (session) {
                val token = ticket.getToken();
                if (token != null) {
                    log.debug(() -> "logging in session %s by password".formatted(session.getId()));
                    request.getSession().setAttribute("authorized", authorizer.bind(ticket));
                    //noinspection SpellCheckingInspection
                    if (isNotEmpty(request.getParameter("rememberme"))) {
                        val cookie = new Cookie("authToken", token);
                        cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(30));
                        response.addCookie(cookie);
                    }
                    response.setContentType(ContentType.JAVA_SERIALIZED_OBJECT.toString());
                    val writer = response.getWriter();
                    writer.print(Pretty.$(JsonMap.singletonMap("success", true)));
                    writer.flush();
                } else {
                    log.debug(() -> "password login failed for %s".formatted(session.getId()));
                    //response.setHeader("WWW-Authenticate", String.format("Basic realm=\"%s\"", asset));
                    response.sendError(SC_FORBIDDEN);
                }
            }
        }
    }

}

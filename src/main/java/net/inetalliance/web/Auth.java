package net.inetalliance.web;

import net.inetalliance.log.Log;
import net.inetalliance.types.json.JsonMap;
import net.inetalliance.types.json.Pretty;
import net.inetalliance.types.www.ContentType;
import net.inetalliance.util.security.Ticket;
import net.inetalliance.util.security.auth.Authenticator;
import net.inetalliance.util.security.auth.Authorized;
import net.inetalliance.util.security.auth.Authorizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static net.inetalliance.funky.StringFun.isNotEmpty;

public class Auth extends Processor
{
	public static Authenticator authenticator;
	public static Authorizer authorizer;
	public static String asset;
	private static final Pattern logout = Pattern.compile("(.*)/logout");

	@Override public void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
			throws Throwable
	{
		final Matcher matcher = logout.matcher(request.getRequestURI());
		if (matcher.matches())
		{
			final HttpSession session = request.getSession();
			log.debug("logging out %s",session.getId());
			session.removeAttribute("authorized");
			session.invalidate();
			final Cookie cookie = new Cookie("authToken", "");
			cookie.setMaxAge(-1);
			response.addCookie(cookie);
			response.sendRedirect(matcher.group(1));
		}
		else  // login
		{
			final String name = request.getParameter("username");
			final String password = request.getParameter("password");
			final HttpSession session = request.getSession();
			final Ticket ticket = authenticator.login(name, password, session.getId());
			synchronized (session)
			{
				final String token = ticket.getToken();
				if (token != null)
				{
					log.debug("logging in session %s by password", session.getId());
					request.getSession().setAttribute("authorized", authorizer.bind(ticket));
					if (isNotEmpty(request.getParameter("rememberme")))
					{
						final Cookie cookie = new Cookie("authToken", token);
						cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(30));
						response.addCookie(cookie);
					}
					response.setContentType(ContentType.JAVA_SERIALIZED_OBJECT.toString());
					final PrintWriter writer = response.getWriter();
					writer.print(Pretty.$(JsonMap.singletonMap("success", true)));
					writer.flush();
				}
				else
				{
					log.debug("password login failed for %s",session.getId());
					//response.setHeader("WWW-Authenticate", String.format("Basic realm=\"%s\"", asset));
					response.sendError(SC_FORBIDDEN);
				}
			}
		}
	}

	public static boolean isAuthenticated(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException
	{
		log.trace("checking if %s is authenticated",request.getSession().getId());
		final Authorized authorized = getAuthorized(request);
		if (authorized != null)
		{
			log.trace("%s is authenticated",request.getSession().getId());
			return true;
		}
		final Cookie[] cookies = request.getCookies();
		if (cookies != null)
		{
			for (final Cookie cookie : cookies)
			{
				if ("authToken".equals(cookie.getName()))
				{
					log.trace("%s has an authToken",request.getSession().getId());
					final Ticket ticket = authenticator.login(cookie.getValue());
					if (ticket == null || ticket.getToken() == null)
						break;
					else
					{
						log.trace("%s has a valid authToken",request.getSession().getId());
						request.getSession().setAttribute("authorized", authorizer.bind(ticket));
						return true;
					}
				}
			}
		}
		log.trace("%s is not authenticated",request.getSession().getId());
		response.sendError(SC_FORBIDDEN);
		return false;
	}

	public static Authorized getAuthorized(final HttpServletRequest request)
	{
		return (Authorized) request.getSession().getAttribute("authorized");
	}

	private static transient final Log log = Log.getInstance(Auth.class);

}

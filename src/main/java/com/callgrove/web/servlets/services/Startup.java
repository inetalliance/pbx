package com.callgrove.web.servlets.services;

import com.callgrove.*;
import net.inetalliance.amberjack.messages.Authenticator;
import net.inetalliance.beejax.messages.*;
import net.inetalliance.daemonic.*;
import net.inetalliance.log.*;
import net.inetalliance.potion.*;
import net.inetalliance.sql.*;
import net.inetalliance.util.security.auth.impl.*;
import net.inetalliance.web.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.net.*;

public class Startup
		extends Processor {
	private static final transient Log log = Log.getInstance(Startup.class);

	@Override
	public void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
			throws Throwable {

	}

	@Override
	public void destroy() {
		super.destroy();
		Locator.detach();
	}

	@Override
	public void init(final ServletConfig config)
			throws ServletException {
		super.init(config);
		log.info("Starting up %s", config.getServletContext().getContextPath());
		final String dbParam = getInitParameter(config, "db");
		try {
			Locator.attach(new Db(new URI(dbParam)));
		} catch (URISyntaxException e) {
			log.error("could not parse db parameter as uri: %s", dbParam, e);
			throw new ServletException(e);
		} catch (Throwable t) {
			log.error("could not attach to db", t);
			System.exit(1);
		}
		log.info("loading localized messages");
		log.info("registering business objects");
		try {
			Callgrove.register();
		} catch (Throwable t) {
			log.error(t);
			throw new ServletException(t);
		}
		log.info("configuring security");
		Auth.asset = getInitParameter(config, "asset");
		final String authParam = getInitParameter(config, "authenticator");
		try {
			if (authParam == null) {
				log.warning("Proceeding with no authenticator");
			} else {
				Auth.authenticator = new Authenticator(new URI(authParam), Auth.asset);
			}
		} catch (URISyntaxException e) {
			log.error("could not parse authenticator as uri: %s", authParam, e);
			throw new ServletException(e);
		} catch (Throwable t) {
			log.error(t);
			throw new ServletException(t);
		}
		try {
			final boolean dev = RuntimeKeeper.isDevelopment();
			Auth.authorizer = new SimpleAuthorizer(dev, AuthorizedUser.class);
			Callgrove.beejax = MessageServer.$(BeejaxMessageServer.class, getInitParameter(config, "beejaxMessageServer"));
			log.info("%s is ready", config.getServletContext().getContextPath());
		} catch (Throwable t) {
			log.error(t);
			throw new ServletException(t);
		}
	}

}

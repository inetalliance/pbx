package com.callgrove.web.servlets.services;

import com.callgrove.Callgrove;
import net.inetalliance.amberjack.messages.Authenticator;
import net.inetalliance.beejax.messages.BeejaxMessageServer;
import net.inetalliance.daemonic.RuntimeKeeper;
import net.inetalliance.log.Log;
import net.inetalliance.potion.Locator;
import net.inetalliance.potion.MessageServer;
import net.inetalliance.sql.Db;
import net.inetalliance.util.security.auth.impl.AuthorizedUser;
import net.inetalliance.util.security.auth.impl.SimpleAuthorizer;
import net.inetalliance.web.Auth;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class Startup extends Processor
{
	@Override
	public void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response) throws Throwable
	{

	}

	@Override
	public void init(final ServletConfig config) throws ServletException
	{
		super.init(config);
		log.info("Starting up %s", config.getServletContext().getContextPath());
		final String dbParam = getInitParameter(config,"db");
		try
		{
			Locator.attach(new Db(new URI(dbParam)));
		}
		catch (URISyntaxException e)
		{
			log.error("could not parse db parameter as uri: %s", dbParam, e);
			throw new ServletException(e);
		}
		catch (Throwable t)
		{
			log.error("could not attach to db", t);
			System.exit(1);
		}
		log.info("loading localized messages");
		log.info("registering business objects");
		try
		{
			Callgrove.register();
		}
		catch (Throwable t)
		{
			log.error(t);
			throw new ServletException(t);
		}
		log.info("configuring security");
		Auth.asset = getInitParameter(config, "asset");
		final String authParam = getInitParameter(config, "authenticator");
		try
		{
			if (authParam == null)
				log.warning("Proceeding with no authenticator");
			else
				Auth.authenticator = new Authenticator(new URI(authParam), Auth.asset);
		}
		catch (URISyntaxException e)
		{
			log.error("could not parse authenticator as uri: %s", authParam, e);
			throw new ServletException(e);
		}
		catch (Throwable t)
		{
			log.error(t);
			throw new ServletException(t);
		}
		try
		{
			final boolean dev = RuntimeKeeper.isDevelopment();
			Auth.authorizer = new SimpleAuthorizer(dev, AuthorizedUser.class);
			Callgrove.beejax = MessageServer.$(BeejaxMessageServer.class, getInitParameter(config, "beejaxMessageServer"));
			log.info("%s is ready", config.getServletContext().getContextPath());
		}
		catch (Throwable t)
		{
			log.error(t);
			throw new ServletException(t);
		}
	}

	@Override public void destroy()
	{
		super.destroy();
		Locator.detach();
	}

	private static final transient Log log = Log.getInstance(Startup.class);

}

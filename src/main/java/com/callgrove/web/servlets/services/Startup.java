package com.callgrove.web.servlets.services;

import com.ameriglide.phenix.core.Log;
import com.callgrove.Callgrove;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.amberjack.messages.Authenticator;
import net.inetalliance.beejax.messages.BeejaxMessageServer;
import net.inetalliance.cli.Cli;
import net.inetalliance.potion.Locator;
import net.inetalliance.sql.Db;
import net.inetalliance.util.security.auth.impl.AuthorizedUser;
import net.inetalliance.util.security.auth.impl.SimpleAuthorizer;
import net.inetalliance.web.Auth;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;

import java.net.URI;
import java.net.URISyntaxException;

public class Startup
        extends Processor {

    private static final Log log = new Log();

    @Override
    public void $(final HttpMethod method, final HttpServletRequest request,
                  final HttpServletResponse response)
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
        log.info(() -> "Starting up %s".formatted(config.getServletContext().getContextPath()));
        val dbParam = getInitParameter(config, "db");
        try {
            Locator.attach(new Db(new URI(dbParam)));
        } catch (URISyntaxException e) {
            log.error(() -> "could not parse db parameter as uri: %s".formatted(dbParam), e);
            throw new ServletException(e);
        } catch (Throwable t) {
            log.error(() -> "could not attach to db", t);
            System.exit(1);
        }
        log.info(() -> "loading localized messages");
        log.info(() -> "registering business objects");
        try {
            Callgrove.register();
        } catch (Throwable t) {
            log.error(t);
            throw new ServletException(t);
        }
        log.info(() -> "configuring security");
        Auth.asset = getInitParameter(config, "asset");
        val authParam = getInitParameter(config, "authenticator");
        try {
            if (authParam == null) {
                log.warn(() -> "Proceeding with no authenticator");
            } else {
                Auth.authenticator = new Authenticator(new URI(authParam), Auth.asset);
            }
        } catch (URISyntaxException e) {
            log.error(() -> "could not parse authenticator as uri: %s".formatted(authParam), e);
            throw new ServletException(e);
        } catch (Throwable t) {
            log.error(t);
            throw new ServletException(t);
        }
        try {
            val dev = Cli.isDevelopment();
            Auth.authorizer = new SimpleAuthorizer(dev, AuthorizedUser.class);
            val beejaxUrl = getInitParameter(config, "beejaxMessageServer");
            val apiKey = System.getProperty("msg.apiKey", "");
            Callgrove.beejax = new net.inetalliance.beejax.messages.BeejaxApiClient(beejaxUrl, apiKey);
            log.info(() -> "%s is ready".formatted(config.getServletContext().getContextPath()));
        } catch (Throwable t) {
            log.error(t);
            throw new ServletException(t);
        }
    }

}

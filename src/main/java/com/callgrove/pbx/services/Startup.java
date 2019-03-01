package com.callgrove.pbx.services;

import com.callgrove.app.pbx.*;
import com.callgrove.elastix.*;
import com.callgrove.jobs.*;
import com.callgrove.util.*;
import net.inetalliance.angular.*;
import net.inetalliance.cron.*;
import net.inetalliance.log.*;
import org.asteriskjava.live.*;

import javax.servlet.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import static net.inetalliance.angular.AngularServlet.*;

@WebListener
public class Startup
		extends LocatorStartup {
	private static final transient Log log = Log.getInstance(Startup.class);
	static DefaultAsteriskServer asterisk;
	private Thread router;

	@Override
	public void contextInitialized(final ServletContextEvent sce) {
		super.contextInitialized(sce);
		log.info("pbx startup");
		try {
			final ServletContext context = sce.getServletContext();
			final String asteriskParam = getContextParameter(context, "asterisk");

			asterisk = CallRouter.init(new URI(asteriskParam));
			AsteriskFun.init(asterisk);

			this.router = new Thread(() -> {
				try {
					CallRouter.exec(asterisk);
				} catch (final IOException e) {
					log.error(e);
				}
			});
			router.start();
			SipMonitor.add(asterisk);
			Cron.interval(20, TimeUnit.SECONDS, new CFSync(asterisk));

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
		super.contextDestroyed(sce);
		if (router != null) {
			router.interrupt();
		}
		CallRouter.shutdown();
		if (asterisk != null) {
			asterisk.shutdown();
		}
	}

}

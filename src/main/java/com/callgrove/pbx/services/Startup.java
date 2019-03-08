package com.callgrove.pbx.services;

import static net.inetalliance.angular.AngularServlet.getContextParameter;

import com.callgrove.app.pbx.SipMonitor;
import com.callgrove.elastix.CallRouter;
import com.callgrove.jobs.CFSync;
import com.callgrove.util.AsteriskFun;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;
import net.inetalliance.angular.LocatorStartup;
import net.inetalliance.cron.Cron;
import net.inetalliance.log.Log;
import org.asteriskjava.live.DefaultAsteriskServer;

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

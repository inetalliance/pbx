package com.callgrove.pbx.services;

import com.callgrove.app.pbx.SipMonitor;
import com.callgrove.elastix.CallRouter;
import com.callgrove.jobs.CFSync;
import com.callgrove.util.AsteriskFun;
import net.inetalliance.cron.Cron;
import net.inetalliance.log.Log;
import org.asteriskjava.live.DefaultAsteriskServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class Startup extends com.callgrove.web.servlets.services.Startup {
  public static DefaultAsteriskServer asterisk;

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    log.info("pbx startup");
    try {
      asterisk = CallRouter.init(new URI(config.getInitParameter("asterisk")));
      AsteriskFun.init(asterisk);
      new Thread(() -> {
        try {
          CallRouter.exec(asterisk);
        } catch (final IOException e) {
          log.error(e);
        }
      }).start();
      new Thread(() -> SipMonitor.exec(asterisk)).start();
      Cron.interval(15, TimeUnit.SECONDS, new CFSync(asterisk));

    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private static final transient Log log = Log.getInstance(Startup.class);

}

package com.callgrove.pbx.services;


import com.ameriglide.phenix.core.Log;
import com.callgrove.app.pbx.SipMonitor;
import com.callgrove.elastix.CallRouter;
import com.callgrove.jobs.CFSync;
import com.callgrove.util.AsteriskFun;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.annotation.WebListener;
import lombok.val;
import net.inetalliance.angular.LocatorStartup;
import net.inetalliance.cron.Cron;
import org.asteriskjava.live.DefaultAsteriskServer;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static net.inetalliance.angular.AngularServlet.getContextParameter;

@WebListener
public class Startup
        extends LocatorStartup {

    private static final Log log = new Log();
    static DefaultAsteriskServer asterisk;
    private Thread router;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        super.contextInitialized(sce);
        log.info(() -> "pbx startup");
        try {
            val context = sce.getServletContext();
            val asteriskParam = getContextParameter(context, "asterisk");

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

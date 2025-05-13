package com.callgrove.pbx.services;


import com.ameriglide.phenix.core.Log;
import com.callgrove.obj.Agent;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.potion.Locator;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;
import net.inetalliance.web.errors.InternalServerError;

import java.util.Collections;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;

@WebServlet("/originate")
public class Originate
        extends Processor {

    private static final Log log = new Log();

    @Override
    public void $(final HttpMethod method, final HttpServletRequest request,
                  final HttpServletResponse response)
            throws Throwable {
        response.sendError(SC_OK);
        val from = request.getParameter("from");
        val to = request.getParameter("to");
        log.info("Originate %s->%s", from, to);

        try {
            var agent = Locator.$(new Agent(from));
            //noinspection SpellCheckingInspection
            Startup.asterisk
                    .originateToExtensionAsync(from, "from-internal", to, 1, 15000L, agent.getCallerId(),
                            Collections.singletonMap("INTRACOMPANYROUTE", "YES"), null);

        } catch (Exception e) {
            throw new InternalServerError(e);
        }
    }

}

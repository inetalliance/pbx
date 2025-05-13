package com.callgrove.pbx.services;


import com.ameriglide.phenix.core.Log;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;
import net.inetalliance.web.errors.InternalServerError;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;


@WebServlet("/transfer")
public class Transfer
        extends Processor {

    private static final Log log = new Log();

    @Override
    public void $(final HttpMethod method, final HttpServletRequest request,
                  final HttpServletResponse response)
            throws Throwable {
        response.sendError(SC_OK);
        val agent = request.getParameter("agent");
        val call = request.getParameter("call");
        log.info("Transfer %s->%s", call, agent);

        try {
            val channel = Startup.asterisk.getChannelById(call);
            channel.redirect("from-internal", agent, 1);

        } catch (Exception e) {
            throw new InternalServerError(e);
        }
    }

}

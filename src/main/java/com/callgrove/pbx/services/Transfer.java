package com.callgrove.pbx.services;

import net.inetalliance.log.Log;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;
import net.inetalliance.web.errors.InternalServerError;
import org.asteriskjava.live.AsteriskChannel;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.*;

@WebServlet("/transfer")
public class Transfer
	extends Processor {

	@Override
	public void $(final HttpMethod method, final HttpServletRequest request,
		final HttpServletResponse response) throws Throwable {
		response.sendError(SC_OK);
		final String agent = request.getParameter("agent");
		final String call = request.getParameter("call");
		log.info("Transfer %s->%s", call, agent);

		try {
			final AsteriskChannel channel = Startup.asterisk.getChannelById(call);
			channel.redirect("from-internal", agent, 1);

		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	private static transient final Log log = Log.getInstance(Transfer.class);

}

package com.callgrove.pbx.services;

import net.inetalliance.log.*;
import net.inetalliance.web.*;
import net.inetalliance.web.errors.*;
import org.asteriskjava.live.*;

import javax.servlet.annotation.*;
import javax.servlet.http.*;

import static javax.servlet.http.HttpServletResponse.*;

@WebServlet("/transfer")
public class Transfer
		extends Processor {

	private static transient final Log log = Log.getInstance(Transfer.class);

	@Override
	public void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
			throws Throwable {
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

}

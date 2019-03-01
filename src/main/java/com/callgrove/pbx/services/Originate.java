package com.callgrove.pbx.services;

import com.callgrove.obj.*;
import net.inetalliance.log.*;
import net.inetalliance.potion.*;
import net.inetalliance.web.*;
import net.inetalliance.web.errors.*;

import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.util.*;

import static javax.servlet.http.HttpServletResponse.*;

@WebServlet("/originate")
public class Originate
		extends Processor {

	private static transient final Log log = Log.getInstance(Originate.class);

	@Override
	public void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
			throws Throwable {
		response.sendError(SC_OK);
		final String from = request.getParameter("from");
		final String to = request.getParameter("to");
		log.info("Originate %s->%s", from, to);

		try {
			Agent agent = Locator.$(new Agent(from));
			Startup.asterisk.originateToExtensionAsync(from, "from-internal", to, 1, 15000L, agent.getCallerId(),
			                                           Collections.singletonMap("INTRACOMPANYROUTE", "YES"), null);

		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

}

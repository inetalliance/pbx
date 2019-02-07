package com.callgrove.pbx.services;

import com.callgrove.obj.Agent;
import net.inetalliance.log.Log;
import net.inetalliance.potion.Locator;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;
import net.inetalliance.web.errors.InternalServerError;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static javax.servlet.http.HttpServletResponse.SC_OK;

@WebServlet("/originate")
public class Originate extends Processor
{

	@Override
	public void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
			throws Throwable
	{
		response.sendError(SC_OK);
		final String from = request.getParameter("from");
		final String to = request.getParameter("to");
		log.info("Originate %s->%s", from, to);

		try
		{
			Agent agent = Locator.$(new Agent(from));
			Startup.asterisk.originateToExtensionAsync(from, "from-internal", to, 1, 15000L, agent.getCallerId(),
					Collections.singletonMap("INTRACOMPANYROUTE", "YES"), null);

		}
		catch (Exception e)
		{
			throw new InternalServerError(e);
		}
	}

	private static transient final Log log = Log.getInstance(Originate.class);

}

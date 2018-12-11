package com.callgrove.pbx.services;

import com.callgrove.jobs.Hud;
import com.callgrove.obj.Call;
import com.callgrove.util.AsteriskFun;
import net.inetalliance.log.Log;
import net.inetalliance.potion.Locator;
import net.inetalliance.types.json.JsonMap;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;
import org.asteriskjava.live.AsteriskChannel;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.callgrove.pbx.services.Listen.Action.LISTEN;
import static com.callgrove.pbx.services.Startup.asterisk;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public class Listen extends Processor
{

	public enum Action
	{
		LISTEN, WHISPER
	}

	public Listen()
	{
	}

	@Override
	public void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
			throws Throwable
	{

		response.sendError(SC_OK);
		final Action action = getParam(request, Action.class, "action");
		final String agent = request.getParameter("agent");
		final String manager = request.getParameter("manager");
		final JsonMap json = Hud.currentStatus.getMap(agent);
		final String key = json.get("callId");
		if(key == null) {
			return;
		}
		log.info("%s to %s (%s) channel: %s", action, agent, manager, key);
		final String dial = AsteriskFun.F.dialString.$(asterisk, manager);
		final Call call = Locator.$(new Call(key));
		log.info("call: %s", call);
		try
		{
			final AsteriskChannel channel = asterisk.getChannelById(call.key);
			final String channelName = channel.getLinkedChannel().getName();
			final String options = action == LISTEN ? channel.getName() : String.format("%s|w", channelName);
			asterisk.originateToApplication(dial, "ChanSpy", options, 15000);
		}
		catch (Exception e)
		{
			log.error(e);
		}

	}

	private static final transient Log log = Log.getInstance(Listen.class);

}

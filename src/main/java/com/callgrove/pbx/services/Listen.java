package com.callgrove.pbx.services;

import com.callgrove.jobs.*;
import com.callgrove.obj.*;
import com.callgrove.util.*;
import net.inetalliance.log.*;
import net.inetalliance.potion.*;
import net.inetalliance.types.json.*;
import net.inetalliance.web.*;
import org.asteriskjava.live.*;

import javax.servlet.annotation.*;
import javax.servlet.http.*;

import static com.callgrove.pbx.services.Listen.Action.*;
import static com.callgrove.pbx.services.Startup.*;
import static javax.servlet.http.HttpServletResponse.*;

@WebServlet("/listen")
public class Listen
		extends Processor {

	private static final transient Log log = Log.getInstance(Listen.class);

	public Listen() {
	}

	@Override
	public void $(final HttpMethod method, final HttpServletRequest request, final HttpServletResponse response)
			throws Throwable {

		response.sendError(SC_OK);
		final Action action = getParam(request, "action", Action.class);
		final String agent = request.getParameter("agent");
		final String manager = request.getParameter("manager");
		final JsonMap json = Hud.currentStatus.getMap(agent);
		final String key = json.get("callId");
		if (key == null) {
			return;
		}
		log.info("%s to %s (%s) channel: %s", action, agent, manager, key);
		final String dial = AsteriskFun.getDialString(manager);
		final Call call = Locator.$(new Call(key));
		log.info("call: %s", call);
		try {
			final AsteriskChannel channel = asterisk.getChannelById(call.key);
			final String channelName = channel.getLinkedChannel().getName();
			final String options = action == LISTEN ? channel.getName() : String.format("%s|w", channelName);
			asterisk.originateToApplication(dial, "ChanSpy", options, 15000);
		} catch (Exception e) {
			log.error(e);
		}

	}

	public enum Action {
		LISTEN,
		WHISPER
	}

}

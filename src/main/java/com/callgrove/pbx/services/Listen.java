package com.callgrove.pbx.services;

import com.ameriglide.phenix.core.Log;
import com.callgrove.jobs.Hud;
import com.callgrove.obj.Call;
import com.callgrove.util.AsteriskFun;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.potion.Locator;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;

import static com.callgrove.pbx.services.Listen.Action.LISTEN;
import static com.callgrove.pbx.services.Startup.asterisk;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;

@WebServlet("/listen")
public class Listen
        extends Processor {

    private static final Log log = new Log();

    public Listen() {
    }

    @Override
    public void $(final HttpMethod method, final HttpServletRequest request,
                  final HttpServletResponse response)
            throws Throwable {

        response.sendError(SC_OK);
        val action = getParam(request, "action", Action.class);
        val agent = request.getParameter("agent");
        val manager = request.getParameter("manager");
        val json = Hud.currentStatus.getMap(agent);
        val key = json.get("callId");
        if (key == null) {
            return;
        }
        log.info("%s to %s (%s) channel: %s", action, agent, manager, key);
        val dial = AsteriskFun.getDialString(manager);
        val call = Locator.$(new Call(key));
        log.info("call: %s", call);
        try {
            val channel = asterisk.getChannelById(call.key);
            val channelName = channel.getLinkedChannel().getName();
            val options =
                    action == LISTEN ? channel.getName() : String.format("%s,w", channelName);
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

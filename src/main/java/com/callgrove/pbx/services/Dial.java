package com.callgrove.pbx.services;

import com.ameriglide.phenix.core.Log;
import com.callgrove.elastix.CallRouter;
import com.callgrove.obj.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import net.inetalliance.potion.Locator;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;
import net.inetalliance.web.errors.InternalServerError;
import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.CallerId;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.live.OriginateCallback;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ameriglide.phenix.core.Strings.isNotEmpty;
import static com.callgrove.elastix.CallRouter.resolved;
import static com.callgrove.pbx.services.Startup.asterisk;
import static com.callgrove.types.CallDirection.OUTBOUND;
import static com.callgrove.types.Resolution.*;
import static com.callgrove.types.SaleSource.PHONE_CALL;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static java.lang.String.format;
import static net.inetalliance.potion.Locator.create;
import static net.inetalliance.potion.Locator.update;
import static org.asteriskjava.live.ChannelState.HUNGUP;

@WebServlet("/dial")
public class Dial
        extends Processor {

    private static final Log log = new Log();
    private static final AtomicInteger id = new AtomicInteger(0);

    @Override
    public void $(final HttpMethod method, final HttpServletRequest request,
                  final HttpServletResponse response)
            throws Throwable {
        val id = Dial.id.getAndIncrement();
        response.sendError(SC_OK);
        val agent = request.getParameter("agent");
        val effectiveQueue = request.getParameter("effectiveQueue");
        val site = request.getParameter("site");
        val contact = request.getParameter("contact");
        val cidName = request.getParameter("cidName");
        val cidNumber = request.getParameter("cidNumber");
        val number = request.getParameter("number");
        val opportunity = request.getParameter("opportunity");
        log.info(() ->
                "[%d] Dial %s(%s) for %s on %s(%s) cid: %s<%s>".formatted(id, number, contact, agent, site,
                        effectiveQueue,
                        cidName, cidNumber));
        try {
            val dial = format("SIP/%s", agent);

            asterisk.originateToExtensionAsync(dial, "from-internal", number, 1, 30000,
                    new CallerId(cidName, cidNumber),
                    null, new OriginateCallback() {
                        @Override
                        public void onDialing(final AsteriskChannel channel) {
                            val call = new Call(channel.getId());
                            call.setCreated(LocalDateTime.now());
                            call.setCallerId(new com.callgrove.types.CallerId(cidName, cidNumber));
                            call.setAgent(Locator.$(new Agent(agent)));
                            log.debug(() -> "[%d] agent (param) -> %s".formatted(id, printAgent(call.getAgent())));
                            if (agent == null) {
                                call.setAgent(Locator.$(new Agent(cidNumber)));
                                log.debug(() -> "[%d] agent (cid) -> %s".formatted(id, printAgent(call.getAgent())));
                            }
                            if (call.getAgent() == null) {
                                call.setAgent(
                                        Locator.$1(Agent.withLastName(call.getCallerId().getName().split(",")[0])));
                                log.debug(() -> "[%d] agent (name) -> %s".formatted(id, printAgent(call.getAgent())));
                            }
                            if (isNotEmpty(site)) {
                                call.setSite(Locator.$(new Site(Integer.valueOf(site))));
                            }
                            if (isNotEmpty(opportunity)) {
                                val opp = Locator.$(new Opportunity(Integer.valueOf(opportunity)));
                                call.setOpportunity(opp);
                                if (opp != null) {
                                    call.setContact(opp.getContact());
                                    call.setSource(opp.getSource());
                                    if (call.getAgent() == null) {
                                        call.setAgent(opp.getAssignedTo());
                                        log.debug(() -> "[%d] agent (opp) -> %s".formatted(id, printAgent(call.getAgent())));
                                    }
                                }
                            }
                            call.setSource(PHONE_CALL);

                            if (isNotEmpty(effectiveQueue)) {
                                call.setQueue(Locator.$(new Queue(effectiveQueue)));
                            }

                            call.setResolution(ACTIVE);
                            call.setDirection(OUTBOUND);
                            create("dial", call);

                            val meta = new CallRouter.Meta(call);
                            meta.agent = call.getAgent();

                            channel.addPropertyChangeListener(new PropertyChangeListener() {
                                @Override
                                public void propertyChange(final PropertyChangeEvent evt) {
                                    log.trace(() -> "[%s] %s".formatted(evt.toString(), call.key));
                                    switch (evt.getPropertyName()) {
                                        case "dialedChannel": {
                                            var dialedChannel = (AsteriskChannel) evt.getNewValue();
                                            val segment = new Segment(call, dialedChannel.getId());
                                            segment.setCreated(LocalDateTime.now());
                                            var dialedAgent = Locator
                                                    .$(new Agent(dialedChannel.getCallerId().getNumber()));
                                            segment.setAgent(
                                                    dialedAgent == null ? Locator.$(new Agent(agent)) : dialedAgent);
                                            log.debug(() -> "[%d:%s] agent (segment) -> %s".formatted(id, call.key,
                                                    printAgent(segment.getAgent())));
                                            segment.setCallerId(new com.callgrove.types.CallerId("", number));
                                            create("dial", segment);
                                            update(call, "dial", copy -> {
                                                val agent1 = segment.getAgent();
                                                if (agent1 != null) {
                                                    copy.setAgent(agent1);
                                                    log.debug(() -> "[%d:%s] agent (dial) -> %s".formatted(id, copy.key,
                                                            printAgent(copy.getAgent())));
                                                }
                                            });
                                            break;
                                        }
                                        case "state": {
                                            val source = (AsteriskChannel) evt.getSource();
                                            if (HUNGUP.equals(evt.getNewValue()) && !source.getName()
                                                    .endsWith("<ZOMBIE>")) {
                                                log.debug(() -> "[%d:%s] hung up normally".formatted(id, call.key));
                                                update(call, "dial", copy -> {
                                                    if (!resolved.contains(copy.getResolution())) {
                                                        // look and see if we just came from VoiceMail
                                                        // (history-1 = hangup,
                                                        //  history-2 = previous app)
                                                        val history = source
                                                                .getExtensionHistory();
                                                        if (history.size() > 2 && "macro-vm".equals(
                                                                history.get(history.size() - 2).getExtension().getContext())) {
                                                            copy.setResolution(VOICEMAIL);
                                                        } else {
                                                            copy.setResolution(ANSWERED);
                                                        }
                                                    }
                                                    copy.setDuration(Duration.between(copy.getCreated(), LocalDateTime.now()));
                                                });
                                            }
                                            break;
                                        }
                                        case "name": {
                                            if (((String) evt.getNewValue()).endsWith("<ZOMBIE>")) {
                                                log.debug(() -> "[%d:%s] transfer, original name was: %s".formatted(id, call.key,
                                                        channel.getName()));
                                                var masq = asterisk.getChannelByName(channel.getName());
                                                masq.addPropertyChangeListener(this);
                                                val segment = new Segment(call, masq.getDialedChannel().getId());
                                                segment.setCreated(LocalDateTime.now());
                                                segment.setAnswered(LocalDateTime.now());
                                                segment.setAgent(Locator
                                                        .$(new Agent(masq.getDialedChannel().getCallerId().getNumber())));
                                                log.debug(() -> "[%d:%s] agent (xfer segment) -> %s".formatted(id, call.key,
                                                        printAgent(call.getAgent())));
                                                segment.setCallerId(
                                                        new com.callgrove.types.CallerId(meta.agent.getLastNameFirstInitial(),
                                                                meta.agent.key));
                                                create("dial", segment);
                                                meta.agent = segment.getAgent();
                                                update(call, "dial", copy -> {
                                                    val agent12 = segment.getAgent();
                                                    if (agent12 != null) {
                                                        copy.setAgent(agent12);
                                                        log.debug(() -> "[%d:%s] agent (xfer call) -> %s".formatted(id, copy.key,
                                                                printAgent(copy.getAgent())));
                                                    }
                                                    copy.setResolution(ACTIVE);
                                                });
                                            }
                                            break;
                                        }
                                        case "linkedChannel": {
                                            val oldValue = (AsteriskChannel) evt.getOldValue();
                                            val newValue = (AsteriskChannel) evt.getNewValue();
                                            log.trace(() -> "[%d:%s] link: %s -> %s".formatted(id, call.key,
                                                    oldValue == null ? null : oldValue.getId(),
                                                    newValue == null ? null : newValue.getId()));
                                            if (newValue == null) { // unlinking a channel
                                                if (oldValue == null) {
                                                    throw new NullPointerException();
                                                }
                                                val segment = Locator.$(new Segment(call, oldValue.getId()));
                                                if (segment == null) {
                                                    log.error(() -> "[%d:%s] could not find segment object %s".formatted(id, call.key,
                                                            oldValue.getId()));
                                                } else {
                                                    update(segment, "dial", copy -> {
                                                        copy.setEnded(LocalDateTime.now());
                                                    });
                                                    update(call, "dial", copy -> {
                                                        if (meta.agent != null) {
                                                            copy.setAgent(meta.agent);
                                                            log.debug(() -> "[%d:%s] agent (link) -> %s".formatted(id, copy.key,
                                                                    printAgent(copy.getAgent())));
                                                        }
                                                        copy.setDuration(Duration.between(copy.getCreated(), LocalDateTime.now()));
                                                    });
                                                }
                                            } else { // linking to the new channel
                                                val segment = Locator.$(new Segment(call, newValue.getId()));
                                                if (segment == null) {
                                                    log.error(() -> "[%d:%s] could not find segment object %s".formatted(id, call.key,
                                                            newValue.getId()));
                                                } else {
                                                    update(segment, "dial", copy -> {
                                                        copy.setAnswered(LocalDateTime.now());
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            });

                        }

                        @Override
                        public void onSuccess(final AsteriskChannel channel) {

                        }

                        @Override
                        public void onNoAnswer(final AsteriskChannel channel) {
                            log.debug(() -> "[%d] agent didn't answer".formatted(id));

                        }

                        @Override
                        public void onBusy(final AsteriskChannel channel) {
                            log.debug(() -> "[%d] agent's extension was busy".formatted(id));

                        }

                        @Override
                        public void onFailure(final LiveException cause) {
                            log.debug(() -> "[%d] originate failed, %s".formatted(id, cause.getMessage()));
                        }
                    });

        } catch (Exception e) {
            throw new InternalServerError(e);
        }

    }

    private String printAgent(final Agent agent) {
        return agent == null ? null : agent.getLastNameFirstInitial();
    }

}

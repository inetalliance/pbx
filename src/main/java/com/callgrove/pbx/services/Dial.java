package com.callgrove.pbx.services;

import static com.callgrove.elastix.CallRouter.resolved;
import static com.callgrove.pbx.services.Startup.asterisk;
import static com.callgrove.types.CallDirection.OUTBOUND;
import static com.callgrove.types.Resolution.ACTIVE;
import static com.callgrove.types.Resolution.ANSWERED;
import static com.callgrove.types.Resolution.VOICEMAIL;
import static com.callgrove.types.SaleSource.PHONE_CALL;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static net.inetalliance.funky.StringFun.isNotEmpty;
import static net.inetalliance.potion.Locator.create;
import static net.inetalliance.potion.Locator.update;
import static org.asteriskjava.live.ChannelState.HUNGUP;

import com.callgrove.elastix.CallRouter;
import com.callgrove.obj.Agent;
import com.callgrove.obj.Call;
import com.callgrove.obj.Opportunity;
import com.callgrove.obj.Queue;
import com.callgrove.obj.Segment;
import com.callgrove.obj.Site;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.inetalliance.log.Log;
import net.inetalliance.potion.Locator;
import net.inetalliance.web.HttpMethod;
import net.inetalliance.web.Processor;
import net.inetalliance.web.errors.InternalServerError;
import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.CallerId;
import org.asteriskjava.live.ExtensionHistoryEntry;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.live.OriginateCallback;
import org.joda.time.DateTime;
import org.joda.time.Duration;

@WebServlet("/dial")
public class Dial
    extends Processor {

  private static transient final Log log = Log.getInstance(Dial.class);
  private static AtomicInteger id = new AtomicInteger(0);

  @Override
  public void $(final HttpMethod method, final HttpServletRequest request,
      final HttpServletResponse response)
      throws Throwable {
    final int id = Dial.id.getAndIncrement();
    response.sendError(SC_OK);
    final String agent = request.getParameter("agent");
    final String effectiveQueue = request.getParameter("effectiveQueue");
    final String site = request.getParameter("site");
    final String contact = request.getParameter("contact");
    final String cidName = request.getParameter("cidName");
    final String cidNumber = request.getParameter("cidNumber");
    final String number = request.getParameter("number");
    final String opportunity = request.getParameter("opportunity");
    log.info(
        format("[%d] Dial %s(%s) for %s on %s(%s) cid: %s<%s>", id, number, contact, agent, site,
            effectiveQueue,
            cidName, cidNumber));
    try {
      final String dial = format("SIP/%s", agent);

      asterisk.originateToExtensionAsync(dial, "from-internal", number, 1, 30000,
          new CallerId(cidName, cidNumber),
          null, new OriginateCallback() {
            @Override
            public void onDialing(final AsteriskChannel channel) {
              final Call call = new Call(channel.getId());
              call.setCreated(new DateTime());
              call.setCallerId(new com.callgrove.types.CallerId(cidName, cidNumber));
              call.setAgent(Locator.$(new Agent(agent)));
              log.debug("[%d] agent (param) -> %s", id, printAgent(call.getAgent()));
              if (agent == null) {
                call.setAgent(Locator.$(new Agent(cidNumber)));
                log.debug("[%d] agent (cid) -> %s", id, printAgent(call.getAgent()));
              }
              if (call.getAgent() == null) {
                call.setAgent(
                    Locator.$1(Agent.withLastName(call.getCallerId().getName().split(",")[0])));
                log.debug("[%d] agent (name) -> %s", id, printAgent(call.getAgent()));
              }
              if (isNotEmpty(site)) {
                call.setSite(Locator.$(new Site(Integer.valueOf(site))));
              }
              if (isNotEmpty(opportunity)) {
                final Opportunity opp = Locator.$(new Opportunity(Integer.valueOf(opportunity)));
                call.setOpportunity(opp);
                if (opp != null) {
                  call.setContact(opp.getContact());
                  call.setSource(opp.getSource());
                  if (call.getAgent() == null) {
                    call.setAgent(opp.getAssignedTo());
                    log.debug("[%d] agent (opp) -> %s", id, printAgent(call.getAgent()));
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

              final CallRouter.Meta meta = new CallRouter.Meta(call);
              meta.agent = call.getAgent();

              channel.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                  log.trace("[%s] " + evt.toString(), call.key);
                  switch (evt.getPropertyName()) {
                    case "dialedChannel": {
                      AsteriskChannel dialedChannel = (AsteriskChannel) evt.getNewValue();
                      final Segment segment = new Segment(call, dialedChannel.getId());
                      segment.setCreated(new DateTime());
                      Agent dialedAgent = Locator
                          .$(new Agent(dialedChannel.getCallerId().getNumber()));
                      segment.setAgent(
                          dialedAgent == null ? Locator.$(new Agent(agent)) : dialedAgent);
                      log.debug("[%d:%s] agent (segment) -> %s", id, call.key,
                          printAgent(segment.getAgent()));
                      segment.setCallerId(new com.callgrove.types.CallerId("", number));
                      create("dial", segment);
                      update(call, "dial", copy -> {
                        final Agent agent1 = segment.getAgent();
                        if (agent1 != null) {
                          copy.setAgent(agent1);
                          log.debug("[%d:%s] agent (dial) -> %s", id, copy.key,
                              printAgent(copy.getAgent()));
                        }
                      });
                      break;
                    }
                    case "state": {
                      final AsteriskChannel source = (AsteriskChannel) evt.getSource();
                      if (HUNGUP.equals(evt.getNewValue()) && !source.getName()
                          .endsWith("<ZOMBIE>")) {
                        log.debug("[%d:%s] hung up normally", id, call.key);
                        update(call, "dial", copy -> {
                          if (!resolved.contains(copy.getResolution())) {
                            // look and see if we just came from VoiceMail (history-1 = hangup, history-2 = previous
                            // app)
                            final List<ExtensionHistoryEntry> history = source
                                .getExtensionHistory();
                            if (history.size() > 2 && "macro-vm".equals(
                                history.get(history.size() - 2).getExtension().getContext())) {
                              copy.setResolution(VOICEMAIL);
                            } else {
                              copy.setResolution(ANSWERED);
                            }
                          }
                          copy.setDuration(new Duration(copy.getCreated(), new DateTime()));
                        });
                      }
                      break;
                    }
                    case "name": {
                      if (((String) evt.getNewValue()).endsWith("<ZOMBIE>")) {
                        log.debug("[%d:%s] transfer, original name was: %s", id, call.key,
                            channel.getName());
                        AsteriskChannel masq = asterisk.getChannelByName(channel.getName());
                        masq.addPropertyChangeListener(this);
                        final Segment segment = new Segment(call, masq.getDialedChannel().getId());
                        segment.setCreated(new DateTime());
                        segment.setAnswered(new DateTime());
                        segment.setAgent(Locator
                            .$(new Agent(masq.getDialedChannel().getCallerId().getNumber())));
                        log.debug("[%d:%s] agent (xfer segment) -> %s", id, call.key,
                            printAgent(call.getAgent()));
                        segment.setCallerId(
                            new com.callgrove.types.CallerId(meta.agent.getLastNameFirstInitial(),
                                meta.agent.key));
                        create("dial", segment);
                        meta.agent = segment.getAgent();
                        update(call, "dial", copy -> {
                          final Agent agent12 = segment.getAgent();
                          if (agent12 != null) {
                            copy.setAgent(agent12);
                            log.debug("[%d:%s] agent (xfer call) -> %s", id, copy.key,
                                printAgent(copy.getAgent()));
                          }
                          copy.setResolution(ACTIVE);
                        });
                      }
                      break;
                    }
                    case "linkedChannel": {
                      final AsteriskChannel oldValue = (AsteriskChannel) evt.getOldValue();
                      final AsteriskChannel newValue = (AsteriskChannel) evt.getNewValue();
                      log.trace("[%d:%s] link: %s -> %s", id, call.key,
                          oldValue == null ? null : oldValue.getId(),
                          newValue == null ? null : newValue.getId());
                      if (newValue == null) { // unlinking a channel
                        assert oldValue != null;
                        final Segment segment = Locator.$(new Segment(call, oldValue.getId()));
                        if (segment == null) {
                          log.error("[%d:%s] could not find segment object %s", id, call.key,
                              oldValue.getId());
                        } else {
                          update(segment, "dial", copy -> {
                            copy.setEnded(new DateTime());
                          });
                          update(call, "dial", copy -> {
                            if (meta.agent != null) {
                              copy.setAgent(meta.agent);
                              log.debug("[%d:%s] agent (link) -> %s", id, copy.key,
                                  printAgent(copy.getAgent()));
                            }
                            copy.setDuration(new Duration(copy.getCreated(), new DateTime()));
                          });
                        }
                      } else { // linking to new channel
                        final Segment segment = Locator.$(new Segment(call, newValue.getId()));
                        if (segment == null) {
                          log.error("[%d:%s] could not find segment object %s", id, call.key,
                              newValue.getId());
                        } else {
                          update(segment, "dial", copy -> {
                            copy.setAnswered(new DateTime());
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
              log.debug("[%d] agent didn't answer", id);

            }

            @Override
            public void onBusy(final AsteriskChannel channel) {
              log.debug("[%d] agent's etension was busy", id);

            }

            @Override
            public void onFailure(final LiveException cause) {
              log.debug("[%d] originate failed, %s", id, cause.getMessage());
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

package org.ijsberg.iglu.server.http.module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.event.EventBus;
import org.ijsberg.iglu.server.http.WebTrafficEvent;
import org.ijsberg.iglu.server.http.WebTrafficMonitor;

import static org.ijsberg.iglu.server.http.WebTrafficEvent.WebTrafficEventType.SUSPECTED_HACKING_ATTEMPT;

public class WebTrafficMonitorImpl implements WebTrafficMonitor {

    private String assetId;
    private EventBus eventBus;

    public WebTrafficMonitorImpl(String assetId) {
        this.assetId = assetId;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private boolean testMessageSent = false;

    @Override
    public boolean allowRequest(HttpServletRequest req, HttpServletResponse resp) {
        return true;
    }

    @Override
    public boolean allowResponse(HttpServletRequest req, HttpServletResponse resp) {
        if(!testMessageSent) {
            testMessageSent = true;
            eventBus.publish(new WebTrafficEvent(SUSPECTED_HACKING_ATTEMPT, assetId, "this is a test message from " + WebTrafficMonitorImpl.class.getSimpleName()));
        }
        return true;
    }
}

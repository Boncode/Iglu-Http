package org.ijsberg.iglu.server.http;

import org.ijsberg.iglu.event.model.BasicEvent;
import org.ijsberg.iglu.event.model.EventType;

import java.time.Instant;

public class WebTrafficEvent extends BasicEvent {
    private String message;

    public enum WebTrafficEventType implements EventType {

        SUSPECTED_HACKING_ATTEMPT;

        @Override
        public String getId() {
            return name();
        }

        @Override
        public String getLabel() {
            return getEventTypeLabel(this);
        }
    }


    public WebTrafficEvent(WebTrafficEventType type, String assetId, String message) {
        super(type, Instant.now(), assetId);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}

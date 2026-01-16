package org.ijsberg.iglu.server.facilities;

import org.ijsberg.iglu.event.model.BasicEvent;
import org.ijsberg.iglu.event.model.EventType;

import java.time.Instant;

public class FileExchangeEvent extends BasicEvent {

    private String message;

    public enum FileExchangeEventType implements EventType {

        FILE_UPLOADED,
        FILE_DOWNLOADED;

        @Override
        public String getId() {
            return name();
        }

        @Override
        public String getLabel() {
            return getEventTypeLabel(this);
        }
    }


    public FileExchangeEvent(EventType type, String assetId, String message) {
        super(type, Instant.now(), assetId);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

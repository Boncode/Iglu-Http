package org.ijsberg.iglu.server.http.module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.configuration.Startable;
import org.ijsberg.iglu.configuration.component.ApplicationSettingsManager;
import org.ijsberg.iglu.configuration.model.WebTrafficSettingsDto;
import org.ijsberg.iglu.event.EventBus;
import org.ijsberg.iglu.event.EventListener;
import org.ijsberg.iglu.event.model.Event;
//import org.ijsberg.iglu.event.monitoring.MonitorEvent;
import org.ijsberg.iglu.event.monitoring.MonitorEvent;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.scheduling.Pageable;
import org.ijsberg.iglu.server.http.WebTrafficEvent;
import org.ijsberg.iglu.server.http.WebTrafficMonitor;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.dataanalysis.OccurrenceRecord;
import org.ijsberg.iglu.util.time.TimePeriod;
import org.ijsberg.iglu.util.time.TimeUnit;

import javax.management.monitor.Monitor;
import java.util.*;

import static org.ijsberg.iglu.configuration.model.IpAccessPolicy.DISABLED;
import static org.ijsberg.iglu.server.http.WebTrafficEvent.WebTrafficEventType.SUSPECTED_HACKING_ATTEMPT;
import static org.ijsberg.iglu.util.http.HttpSupport.getClientIpAddress;

public class WebTrafficMonitorImpl implements WebTrafficMonitor, Startable, Pageable, EventListener {

    //TODO trustedIps

    private String assetId;
    private EventBus eventBus;
    private boolean isStarted;
    //private HashSet<String> suspicousIps = new HashSet<>();
    private Map<String, OccurrenceRecord> accessDeniedRecordsByIp = new HashMap<>();

    public WebTrafficMonitorImpl(String assetId) {
        this.assetId = assetId;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    //private boolean testMessageSent = false;

    private ApplicationSettingsManager applicationSettingsManager;

    public void setApplicationSettingsManager(ApplicationSettingsManager applicationSettingsManager) {
        this.applicationSettingsManager = applicationSettingsManager;
    }

    @Override
    public boolean allowRequest(HttpServletRequest req, HttpServletResponse resp) {
        String clientIpAddress = getClientIpAddress(req);
        return /*!suspicousIps.contains(clientIpAddress) &&*/ applicationSettingsManager.getWebTrafficSettings().isAllowed(clientIpAddress);
    }

    @Override
    public void checkResponse(HttpServletRequest req, HttpServletResponse resp) {
        int status = resp.getStatus();
        if(status != 200) {
            //if(!isDevToolRequest(req)) {
                String clientIpAddress = getClientIpAddress(req);
                OccurrenceRecord record = getAccessDeniedRecord(clientIpAddress);
                record.recordOccurrence(req.getRequestURI());
                System.out.println(new LogEntry("HTTP request noticed: " + clientIpAddress + " " + status + " '" + req.getRequestURI() + "' " + req.getHeader("User-Agent")));
                if(!record.isNrOccurrencesBelow(5, new TimePeriod(5, TimeUnit.SECOND))) {
                    //System.out.println("=======================================================================");
                    //System.out.println(" SUSPECTED HACKING ATTEMPT " + clientIpAddress);
                    //System.out.println("=======================================================================");
                    List<String> details = record.getDetails();
                    handleSuspiciousIp(clientIpAddress);
                    System.out.println(new LogEntry(Level.CRITICAL, "suspected hacking attempt from " + clientIpAddress + ", requesting " + req.getPathInfo()));
                    eventBus.publish(new WebTrafficEvent(SUSPECTED_HACKING_ATTEMPT, assetId, "suspected hacking attempt from " + clientIpAddress + ", requesting " + CollectionSupport.format(details, ", ")));
                }
            //}
        }
    }

    private void handleSuspiciousIp(String clientIpAddress) {
        //suspicousIps.add(clientIpAddress);
        WebTrafficSettingsDto webTrafficSettings = applicationSettingsManager.getWebTrafficSettings();
//        if(webTrafficSettings.getIpAccessPolicy() != DISABLED) {
            webTrafficSettings.addToBlackList(clientIpAddress);
            applicationSettingsManager.saveWebTrafficSettings(webTrafficSettings);
//        }
    }

    private OccurrenceRecord getAccessDeniedRecord(String ip) {
        OccurrenceRecord record = accessDeniedRecordsByIp.get(ip);
        if(record == null) {
            record = new OccurrenceRecord();
            accessDeniedRecordsByIp.put(ip, record);
        }
        return record;
    }

    @Override
    public void start() {
        eventBus.subscribeToAll(this);
        isStarted = true;
    }

    @Override
    public int getPageIntervalInMinutes() {
        return 2;
    }

    @Override
    public int getPageOffsetInMinutes() {
        return 0;
    }

    @Override
    public void onPageEvent(long officialTime) {
        //cleanup
       // suspicousIps.clear();
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void stop() {
        isStarted = false;
    }

    @Override
    public void onEvent(Event event) {
        if(event instanceof MonitorEvent) {
            MonitorEvent monitorEvent = (MonitorEvent) event;
            if(monitorEvent.getRemoteEventTypeId().equals(SUSPECTED_HACKING_ATTEMPT.getId())) {
                System.out.println(new LogEntry("Suspected hacking attempt from " + monitorEvent.getRemoteEventTypeId()));
                eventBus.publish(new WebTrafficEvent(SUSPECTED_HACKING_ATTEMPT, assetId,
                    "suspected hacking attempt at " + Date.from(monitorEvent.getTimestampUtc()) + " at server " + monitorEvent.getRemoteSystemId() + " with message: " + monitorEvent.getRemoteMessage()));
            }
        }
    }
}

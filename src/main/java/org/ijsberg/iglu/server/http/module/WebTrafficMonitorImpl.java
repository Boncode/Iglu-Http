package org.ijsberg.iglu.server.http.module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.configuration.Startable;
import org.ijsberg.iglu.event.EventBus;
import org.ijsberg.iglu.scheduling.Pageable;
import org.ijsberg.iglu.server.http.WebTrafficMonitor;
import org.ijsberg.iglu.util.dataanalysis.OccurrenceRecord;
import org.ijsberg.iglu.util.time.TimePeriod;
import org.ijsberg.iglu.util.time.TimeUnit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.ijsberg.iglu.util.http.HttpSupport.getClientIpAddress;

public class WebTrafficMonitorImpl implements WebTrafficMonitor, Startable, Pageable {

    //TODO trustedIps

    private String assetId;
    private EventBus eventBus;
    private boolean isStarted;
    private HashSet<String> suspicousIps = new HashSet<>();
    private Map<String, OccurrenceRecord> accessDeniedRecordsByIp = new HashMap<>();

    public WebTrafficMonitorImpl(String assetId) {
        this.assetId = assetId;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    private boolean testMessageSent = false;

    @Override
    public boolean allowRequest(HttpServletRequest req, HttpServletResponse resp) {
        String clientIpAddress = getClientIpAddress(req);
        return !suspicousIps.contains(clientIpAddress);
    }

    @Override
    public boolean allowResponse(HttpServletRequest req, HttpServletResponse resp) {
        int status = resp.getStatus();
        if(status != 200) {
            if(!isDevToolRequest(req)) {
                String clientIpAddress = getClientIpAddress(req);
                OccurrenceRecord record = getAccessDeniedRecord(clientIpAddress);
                record.recordOccurrence(req.getPathInfo());
                System.out.println("--> " + req.getPathInfo() + ":" + status + " : " + record.getNrOccurrences());
                if(!record.isNrOccurrencesBelow(5, new TimePeriod(5, TimeUnit.SECOND))) {
                    System.out.println("=======================================================================");
                    System.out.println(" SUSPECTED HACKING ATTEMPT " + clientIpAddress);
                    System.out.println("=======================================================================");
                    suspicousIps.add(clientIpAddress);
//                  eventBus.publish(new WebTrafficEvent(SUSPECTED_HACKING_ATTEMPT, assetId, "this is a test message from " + WebTrafficMonitorImpl.class.getSimpleName() + ", IP: " + clientIpAddress));
                }
            }
        }
        return true;
    }

    private boolean isDevToolRequest(HttpServletRequest req) {
        String path = req.getPathInfo();
        return path.endsWith("devtools.json") || path.endsWith(".map");
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
        suspicousIps.clear();
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void stop() {
        isStarted = false;
    }
}

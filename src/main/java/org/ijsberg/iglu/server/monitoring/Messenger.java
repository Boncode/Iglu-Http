package org.ijsberg.iglu.server.monitoring;

import org.ijsberg.iglu.access.*;
import org.ijsberg.iglu.configuration.Startable;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.mail.MailClient;
import org.ijsberg.iglu.scheduling.Pageable;
import org.ijsberg.iglu.util.properties.IgluProperties;

import javax.mail.MessagingException;
import java.net.InetAddress;
import java.util.Properties;

public class Messenger implements Pageable, Startable, EntryPoint {

    private long SYSTEM_SESSION_TIMEOUT_SEC_24_HOURS = 86400;

    private AccessManager accessManager;
    private Request request;
    private Session session;
    private MailClient mailClient;

    private String hostDescription;

    private String xorKey;
    private IgluProperties mailProperties;

    public void setAccessManager(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public Messenger(String xorKey, IgluProperties mailProperties) {
        this.xorKey = xorKey;
        this.mailProperties = mailProperties;
    }

    @Override
    public int getPageIntervalInMinutes() {
        return 1;
    }

    @Override
    public int getPageOffsetInMinutes() {
        return 0;
    }

    @Override
    public void onPageEvent(long officialTime) {
        ((StandardSession)session).updateLastAccessedTime();
        UserMessage userMessage = session.getUser().consumeLatestMessage();
        while (userMessage != null) {
            if (userMessage != null && userMessage instanceof MailMessage) {
                MailMessage mailMessage = (MailMessage) userMessage;
                try {
                    mailClient.sendMail(
                            mailProperties.getProperty("user"),
                            mailProperties.getProperty("user"),
                            mailMessage.getSubject() + hostDescription,
                            mailMessage.getMessageText());
                } catch (MessagingException e) {
                    System.out.println(new LogEntry(Level.CRITICAL, "sending mail with subject " + mailMessage.getSubject() + " failed", e));
                }
            }
            userMessage = session.getUser().consumeLatestMessage();
        }
    }

    private boolean isStarted = false;
    @Override
    public void start() {
        isStarted = true;
        loginAsSystem();
        mailClient = new MailClient(xorKey, mailProperties);
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            hostDescription = " " + inetAddress.getHostName() + " : " + inetAddress.getHostAddress();
        } catch (Exception e) {
            System.out.println(new LogEntry(Level.CRITICAL, "cannot figure out host description", e));
        }
    }

    private void loginAsSystem() {
        request = accessManager.bindRequest(this);
        session = request.getSession(true);
        session.loginAsSystem(SYSTEM_SESSION_TIMEOUT_SEC_24_HOURS);
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public void stop() {
        session.logout();
        accessManager.releaseRequest();
        isStarted = false;
    }

    @Override
    public void onSessionUpdate(Request currentRequest, Session session) {

    }

    @Override
    public void onSessionDestruction(Request currentRequest, Session session) {
        loginAsSystem();
    }

    @Override
    public void exportUserSettings(Request currentRequest, Properties userSettings) {

    }

    @Override
    public void importUserSettings(Request currentRequest, Properties properties) {

    }
}

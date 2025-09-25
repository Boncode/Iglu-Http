package org.ijsberg.iglu.server.monitoring;

import org.ijsberg.iglu.access.*;
import org.ijsberg.iglu.configuration.Startable;
import org.ijsberg.iglu.event.messaging.UserConsumableMessage;
import org.ijsberg.iglu.event.messaging.message.MailMessage;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.mail.SMTPClient;
import org.ijsberg.iglu.scheduling.Pageable;
import org.ijsberg.iglu.util.properties.IgluProperties;

import jakarta.mail.MessagingException;
import java.net.InetAddress;
import java.util.Properties;

public class Messenger implements Pageable, Startable, EntryPoint {

    private final long SYSTEM_SESSION_TIMEOUT_SEC_24_HOURS = 86400;

    private AccessManager accessManager;
    //private Request request;
    private Session session;
    private SMTPClient SMTPClient;

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
        UserConsumableMessage userMessage = session.getUser().consumeLatestMessage();
        while (userMessage != null) {
            if (userMessage instanceof MailMessage) {
                MailMessage mailMessage = (MailMessage) userMessage;
                try {
                    SMTPClient.sendMail(
                            mailProperties.getProperty("user"),
                            mailProperties.getProperty("recipient", mailProperties.getProperty("user")),
                            mailMessage.getSubject() + hostDescription,
                            mailMessage.getMessageText());
                } catch (MessagingException e) {
                    System.out.println(new LogEntry(Level.CRITICAL, "sending mail with subject " + mailMessage.getSubject() + " and text: \"" + mailMessage.getMessageText() + "\" failed", e));
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
        SMTPClient = new SMTPClient(xorKey, mailProperties);
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            hostDescription = " " + inetAddress.getHostName() + " : " + inetAddress.getHostAddress();
        } catch (Exception e) {
            System.out.println(new LogEntry(Level.CRITICAL, "cannot figure out host description", e));
        }
    }

    private void loginAsSystem() {
        //
        //request = accessManager.bindRequest(this);
        //accessManager.createSession()
        session = accessManager.createSession(new Properties());
        session.loginAsSystem("System", SYSTEM_SESSION_TIMEOUT_SEC_24_HOURS);
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

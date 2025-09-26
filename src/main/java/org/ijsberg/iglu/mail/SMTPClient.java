package org.ijsberg.iglu.mail;

import jakarta.mail.internet.*;
import org.ijsberg.iglu.util.mail.WebContentType;
import org.ijsberg.iglu.util.misc.EncodingSupport;

import jakarta.mail.*;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.util.List;
import java.util.Properties;

public class SMTPClient implements MailClient {

    private final Properties properties;
    private Authenticator authenticator;

    public SMTPClient(String xorKey, Properties properties) {
        this.properties = properties;

        if(!IgluProperties.checkKeysMissing(properties, "user", "password")) {

            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            properties.getProperty("user"),
                            EncodingSupport.decodeXor(properties.getProperty("password"), xorKey));
                }
            };
        }
    }

    public void sendHTMLMail(String senderEmailAddress, String[] recipientEmailAddresses, String subject, String messageTxt) throws MessagingException {
        sendMail(senderEmailAddress, recipientEmailAddresses, subject, WebContentType.HTML, messageTxt);
    }

    public void sendMail(String senderEmailAddress, String[] recipientEmailAddresses, String subject, String messageTxt) throws MessagingException {
        sendMail(senderEmailAddress, recipientEmailAddresses, subject, WebContentType.TXT, messageTxt);
    }

    public void sendMail(String senderEmailAddress, String[] recipientEmailAddresses, String subject, WebContentType contentType, String messageTxt) throws MessagingException {
        Session session = Session.getInstance(properties, authenticator);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmailAddress));

        message.setRecipients(
                Message.RecipientType.TO, getInternetAddresses(recipientEmailAddresses));
        message.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(messageTxt, contentType.getContentType());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    private Address[] getInternetAddresses(String[] recipientEmailAddresses) throws AddressException {
        Address[] addresses = new Address[recipientEmailAddresses.length];
        for(int i = 0; i < recipientEmailAddresses.length; i++) {
            addresses[i] = InternetAddress.parse(recipientEmailAddresses[i])[0];
        }
        return addresses;
    }
}

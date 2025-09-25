package org.ijsberg.iglu.mail;

import org.ijsberg.iglu.util.mail.WebContentType;
import org.ijsberg.iglu.util.misc.EncodingSupport;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.ijsberg.iglu.util.properties.IgluProperties;

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

    public void sendHTMLMail(String sender, String recipient, String subject, String messageTxt) throws MessagingException {
        sendMail(sender, recipient, subject, WebContentType.HTML, messageTxt);
    }

    public void sendMail(String sender, String recipient, String subject, String messageTxt) throws MessagingException {
        sendMail(sender, recipient, subject, WebContentType.TXT, messageTxt);
    }

    public void sendMail(String sender, String recipient, String subject, WebContentType contentType, String messageTxt) throws MessagingException {
        Session session = Session.getInstance(properties, authenticator);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));

        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(messageTxt, contentType.getContentType());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);

        Transport.send(message);
    }
}

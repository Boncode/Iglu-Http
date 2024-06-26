package org.ijsberg.iglu.mail;

import org.ijsberg.iglu.util.misc.EncodingSupport;
import org.ijsberg.iglu.util.properties.IgluProperties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

public class MailClient {

    private String xorKey = "'>c,vjk09314id mx:.";
    private Properties properties;
    private Authenticator authenticator;

    public MailClient(String xorKey, Properties properties) {
        this.xorKey = xorKey;
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

    public void sendHtmlMail(String sender, String recipient, String subject, String messageTxt) throws MessagingException {
        sendMail(sender, recipient, subject, "text/html", messageTxt);
    }

    public void sendMail(String sender, String recipient, String subject, String messageTxt) throws MessagingException {
        sendMail(sender, recipient, subject, "text/plain", messageTxt);
    }

    public void sendMail(String sender, String recipient, String subject, String contentType, String messageTxt) throws MessagingException {
        Session session = Session.getInstance(properties, authenticator);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));

        InternetAddress[] addresses = InternetAddress.parse(recipient, false);
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(messageTxt, contentType);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);

        Transport.send(message);
    }

}

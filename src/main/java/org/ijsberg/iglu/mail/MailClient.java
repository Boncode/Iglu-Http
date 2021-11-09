package org.ijsberg.iglu.mail;

import org.ijsberg.iglu.util.misc.EncodingSupport;
import org.ijsberg.iglu.util.properties.IgluProperties;

import javax.mail.*;
import javax.mail.internet.*;
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

    public void sendMail(String sender, String recipient, String subject, String messageTxt) throws MessagingException {
        Session session = Session.getInstance(properties, authenticator);

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(messageTxt, "text/plain");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    public static void main(String[] args) throws MessagingException {



//        System.out.println(EncodingSupport.encodeXor("a_10-$35f-BLi9U2yT", xorKey)); // = RmFSHFtOWAVfHnN4AF11XwFu
//        System.out.println(EncodingSupport.decodeXor("RmFSHFtOWAVfHnN4AF11XwFu", xorKey));
        System.exit(0);
        //return EncodingSupport.decodeXor(, passwordXorEncodingKey);



        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.host", "smtp.transip.email");
        prop.put("mail.smtp.port", "465");
        prop.put("mail.smtp.ssl.trust", "smtp.transip.email");
//a_10-$35f-BLi9U2yT
        // = RmFSHFtOWAVfHnN4AF11XwFu
        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("j.meetsma@boncode.nl", "eUvGVpR2!xW%fZK");
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("j.meetsma@boncode.nl"));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse("jeroen@ijsberg.nl"));
        message.setSubject("Mail Subject");

        String msg = "This is my first email using JavaMailer";

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(msg, "text/plain");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

//        Transport t = session.getTransport("smtp");
//        t.connect();
//        t.sendMessage(message, message.getAllRecipients());

        Transport.send(message);
    }


}

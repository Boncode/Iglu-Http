package org.ijsberg.iglu.mail;

import jakarta.mail.MessagingException;

public interface MailClient {

    void sendMail(String senderEmailAddress, String recipientEmailAddress, String subject, String message) throws MessagingException;

    void sendHTMLMail(String senderEmailAddress, String recipientEmailAddress, String subject, String message) throws MessagingException;
}

package com.champtitles.metabasereportexecutor.notifier;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

public class EmailTransport {
    public void send(Message message) throws MessagingException {
        Transport.send(message);
    }
}

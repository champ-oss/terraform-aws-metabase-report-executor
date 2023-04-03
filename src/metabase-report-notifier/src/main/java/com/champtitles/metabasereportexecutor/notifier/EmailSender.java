package com.champtitles.metabasereportexecutor.notifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.Properties;

public class EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class.getName());
    private final Session session;
    private final String fromAddress;
    private final EmailTransport emailTransport;

    public EmailSender(String smtpHost, String smtpPort, String smtpUser, String smtpPassword, String fromAddress) {
        this(smtpHost, smtpPort, smtpUser, smtpPassword, fromAddress, new EmailTransport());
    }

    EmailSender(String smtpHost, String smtpPort, String smtpUser, String smtpPassword, String fromAddress, EmailTransport emailTransport) {
        Properties smtpProperties = createSmtpProperties(smtpHost, smtpPort, smtpUser, smtpPassword);
        Authenticator smtpAuthenticator = createSmtpAuthenticator(smtpUser, smtpPassword);
        session = Session.getInstance(smtpProperties, smtpAuthenticator);
        this.fromAddress = fromAddress;
        this.emailTransport = emailTransport;
    }

    public void sendEmail(String subject, String[] recipients, String fileName, byte[] xlsxData) {
        Message message = new MimeMessage(session);
        setFromAddress(message);
        setRecipients(message, recipients);
        setSubject(message, subject);
        setAttachment(message, fileName, xlsxData);
        send(message);
    }

    private void send(Message message) {
        try {
            emailTransport.send(message);

        } catch (MessagingException e) {
            logger.error("failed to send email message");
            throw new RuntimeException(e);
        }
    }

    private void setFromAddress(Message message) {
        try {
            message.setFrom(new InternetAddress(fromAddress));

        } catch (MessagingException e) {
            logger.error("invalid from address: {}", fromAddress);
            throw new RuntimeException(e);
        }
    }

    private static void setRecipients(Message message, String[] recipients) {
        for (String recipient : recipients) {
            try {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

            } catch (MessagingException e) {
                logger.error("invalid recipient address: {}", recipient);
                throw new RuntimeException(e);
            }
        }
    }

    private static void setSubject(Message message, String subject) {
        try {
            message.setSubject(subject);

        } catch (MessagingException e) {
            logger.error("invalid subject text: {}", subject);
            throw new RuntimeException(e);
        }
    }

    private static void setAttachment(Message message, String fileName, byte[] xlsxData) {
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(xlsxData, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        Multipart multipart = new MimeMultipart();

        try {
            mimeBodyPart.setDataHandler(new DataHandler(byteArrayDataSource));
            mimeBodyPart.setFileName(fileName);
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);

        } catch (MessagingException e) {
            logger.error("failed to create email attachment from file: {}", fileName);
            throw new RuntimeException(e);
        }

    }

    private static Properties createSmtpProperties(String smtpHost, String smtpPort, String smtpUser, String smtpPassword) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.user", smtpUser);
        properties.put("mail.password", smtpPassword);
        return properties;
    }

    private static Authenticator createSmtpAuthenticator(String smtpUser, String smtpPassword) {
        return new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        };
    }
}

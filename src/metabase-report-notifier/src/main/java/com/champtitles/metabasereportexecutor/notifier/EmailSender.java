package com.champtitles.metabasereportexecutor.notifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
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

    /**
     * @param smtpHost     host of SMTP server
     * @param smtpPort     port of SMTP server
     * @param smtpUser     username to log in to SMTP server
     * @param smtpPassword password to log in to SMTP server
     * @param fromAddress  email address to use as the sender
     */
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

    /**
     * Create an email with a xlsx attachment and send it
     *
     * @param subject    subject line of the email
     * @param recipients list of recipient addresses
     * @param fileName   name of the xlsx file attachment
     * @param xlsxData   contents of the xlsx file attachment
     */
    public void sendEmail(String subject, String[] recipients, String fileName, byte[] xlsxData) {
        logger.info("creating email message");
        Message message = new MimeMessage(session);
        setFromAddress(message);
        setRecipients(message, recipients);
        setSubject(message, subject);
        setAttachment(message, fileName, xlsxData);
        send(message);
    }

    /**
     * Sends the email message using the transport
     *
     * @param message email message to send
     */
    private void send(Message message) {
        try {
            logger.info("sending email");
            emailTransport.send(message);

        } catch (MessagingException e) {
            logger.error("failed to send email message");
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the FROM field in the email message
     *
     * @param message email message
     */
    private void setFromAddress(Message message) {
        try {
            logger.info("using from address: {}", fromAddress);
            message.setFrom(new InternetAddress(fromAddress));

        } catch (MessagingException e) {
            logger.error("invalid from address: {}", fromAddress);
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the list of recipients in the email message
     *
     * @param message    email message
     * @param recipients list of recipient email addresses
     */
    private static void setRecipients(Message message, String[] recipients) {
        for (String recipient : recipients) {
            try {
                logger.info("adding recipient: {}", recipient);
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

            } catch (MessagingException e) {
                logger.error("invalid recipient address: {}", recipient);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Set the subject line in the email message
     *
     * @param message email message
     * @param subject subject line to use
     */
    private static void setSubject(Message message, String subject) {
        try {
            logger.info("setting email subject: {}", subject);
            message.setSubject(subject);

        } catch (MessagingException e) {
            logger.error("invalid subject text: {}", subject);
            throw new RuntimeException(e);
        }
    }

    /**
     * Add the xlsx file attachment to the email message
     *
     * @param message  email message
     * @param fileName name of the xlsx file
     * @param xlsxData data contents of the xlsx file
     */
    private static void setAttachment(Message message, String fileName, byte[] xlsxData) {
        logger.info("creating email attachment for file: {}", fileName);

        try {
            // Create a multipart/alternative child container.
            MimeMultipart msgBody = new MimeMultipart("alternative");

            // Create a wrapper for the HTML and text parts.
            MimeBodyPart wrap = new MimeBodyPart();

            // Define the HTML part.
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent("<html></html>", "text/html; charset=UTF-8");

            // Add the HTML parts to the child container.
            // msgBody.addBodyPart(textPart);
            msgBody.addBodyPart(htmlPart);

            // Add the child container to the wrapper object.
            wrap.setContent(msgBody);

            // Create a multipart/mixed parent container.
            MimeMultipart msg = new MimeMultipart("mixed");

            // Add the parent container to the message.
            message.setContent(msg);

            // Add the multipart/alternative part to the message.
            msg.addBodyPart(wrap);

            // Define the attachment.
            MimeBodyPart att = new MimeBodyPart();
            DataSource fds = new ByteArrayDataSource(xlsxData, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            att.setDataHandler(new DataHandler(fds));
            att.setFileName(fileName);

            // Add the attachment to the message.
            msg.addBodyPart(att);

        } catch (MessagingException e) {
            logger.error("failed to create email attachment from file: {}", fileName);
            throw new RuntimeException(e);
        }

    }

    /**
     * Set SMTP settings in a Properties object
     *
     * @param smtpHost     host of SMTP server
     * @param smtpPort     port of SMTP server
     * @param smtpUser     username to log in to SMTP server
     * @param smtpPassword password to log in to SMTP server
     * @return Properties object
     */
    private static Properties createSmtpProperties(String smtpHost, String smtpPort, String smtpUser, String smtpPassword) {
        logger.info("configuring smtp server properties. host={} port={}", smtpHost, smtpPort);
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.user", smtpUser);
        properties.put("mail.password", smtpPassword);
        return properties;
    }

    /**
     * Create an SMTP Authenticator
     *
     * @param smtpUser     username to log in to SMTP server
     * @param smtpPassword password to log in to SMTP server
     * @return SMTP Authenticator
     */
    private static Authenticator createSmtpAuthenticator(String smtpUser, String smtpPassword) {
        return new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        };
    }
}

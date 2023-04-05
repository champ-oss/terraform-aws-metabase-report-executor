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

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSender.class.getName());
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
    public void sendEmail(String subject, String[] recipients, String htmlBody, String fileName, byte[] xlsxData) {
        Message message = new MimeMessage(session);
        setFromAddress(message);
        setRecipients(message, recipients);
        setSubject(message, subject);
        setContent(message, htmlBody, fileName, xlsxData);
        send(message);
    }

    /**
     * Sends the email message using the transport
     *
     * @param message email message to send
     */
    private void send(Message message) {
        try {
            LOGGER.info("sending email");
            emailTransport.send(message);

        } catch (MessagingException e) {
            LOGGER.error("failed to send email message");
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
            LOGGER.info("using from address: {}", fromAddress);
            message.setFrom(new InternetAddress(fromAddress));

        } catch (MessagingException e) {
            LOGGER.error("invalid from address: {}", fromAddress);
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
                LOGGER.info("adding recipient: {}", recipient);
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

            } catch (MessagingException e) {
                LOGGER.error("invalid recipient address: {}", recipient);
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
            LOGGER.info("setting email subject: {}", subject);
            message.setSubject(subject);

        } catch (MessagingException e) {
            LOGGER.error("invalid subject text: {}", subject);
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the body of the email and add the xlsx file attachment to the message
     *
     * @param message  email message
     * @param htmlBody body of the email formatted with HTML
     * @param fileName name of the xlsx file
     * @param xlsxData data contents of the xlsx file
     */
    private static void setContent(Message message, String htmlBody, String fileName, byte[] xlsxData) {
        LOGGER.info("creating email attachment for file: {}", fileName);

        try {
            // Create the HTML body
            MimeBodyPart mimeBodyPartHtml = new MimeBodyPart();
            mimeBodyPartHtml.setContent(htmlBody, "text/html; charset=UTF-8");
            MimeMultipart mimeMultipartAlternative = new MimeMultipart("alternative");
            mimeMultipartAlternative.addBodyPart(mimeBodyPartHtml);

            // Add the child container to the wrapper object
            MimeBodyPart mimeBodyPartWrapper = new MimeBodyPart();
            mimeBodyPartWrapper.setContent(mimeMultipartAlternative);

            // Create the parent mixed container and add the wrapper container
            MimeMultipart mimeMultipartMixed = new MimeMultipart("mixed");
            mimeMultipartMixed.addBodyPart(mimeBodyPartWrapper);

            // Create the attachment as an XLSX document
            MimeBodyPart mimeBodyPartAttachment = new MimeBodyPart();
            DataSource dataSource = new ByteArrayDataSource(xlsxData, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            mimeBodyPartAttachment.setDataHandler(new DataHandler(dataSource));
            mimeBodyPartAttachment.setFileName(fileName);
            mimeMultipartMixed.addBodyPart(mimeBodyPartAttachment);

            message.setContent(mimeMultipartMixed);

        } catch (MessagingException e) {
            LOGGER.error("failed to create email attachment from file: {}", fileName);
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
        LOGGER.info("configuring smtp server properties. host={} port={}", smtpHost, smtpPort);
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

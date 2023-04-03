package com.champtitles.metabasereportexecutor.notifier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.mail.MessagingException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmailSenderTest {

    @Test
    void sendEmail_CreatesAndSendsEmail() throws MessagingException {

        EmailTransport emailTransport = Mockito.mock(EmailTransport.class);

        EmailSender emailSender = new EmailSender("localhost", "25", "testuser",
                "testpassword", "from@example.com", emailTransport);

        String[] recipients = {"test@example.com"};

        byte[] data = "data".getBytes();

        emailSender.sendEmail("test subject", recipients, "test.xlsx", data);

        Mockito.verify(emailTransport, Mockito.times(1)).send(Mockito.argThat(message -> {
            try {
                Arrays.stream(message.getFrom()).forEach(r -> {
                    assertEquals("from@example.com", r.toString());
                });

                Arrays.stream(message.getAllRecipients()).forEach(r -> {
                    assertEquals("test@example.com", r.toString());
                });

                assertEquals("test subject", message.getSubject());

            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
            return true;
        }));
    }

}

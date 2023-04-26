package com.champtitles.metabasereportexecutor.notifier;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

public class AppTest {

    @InjectMocks
    App app;

    @Mock
    S3Reader s3Reader;

    @Mock
    EmailSender emailSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        app = new App(s3Reader, emailSender);
    }

    @Test
    void handleRequest_returns_successfully() {
        Mockito.when(s3Reader.downloadXlsx(Mockito.any())).thenReturn("data".getBytes());

        SNSEvent snsEvent = new SNSEvent().withRecords(new ArrayList<>() {{
            SNSEvent.SNS sns = new SNSEvent.SNS();
            sns.setMessage("""
                    {
                        "Records": [
                            {
                                "s3": {
                                    "object": {
                                        "key": "2023/04/04/12/card-1_2011-12-03T10_15_30.xlsx"
                                    }
                                }
                            }
                        ]
                    }
                    """);
            SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
            record.setSns(sns);
            add(record);
        }});
        app.handleRequest(snsEvent, null);

        Mockito.verify(s3Reader).downloadXlsx("2023/04/04/12/card-1_2011-12-03T10_15_30.xlsx");

        Mockito.verify(emailSender).sendEmail(
                Mockito.contains("Test For "),
                Mockito.eq(new String[]{"test@example.com"}),
                Mockito.eq("<html></html>"),
                Mockito.eq("card-1_2011-12-03T10_15_30.xlsx"),
                Mockito.eq("data".getBytes()));
    }
}

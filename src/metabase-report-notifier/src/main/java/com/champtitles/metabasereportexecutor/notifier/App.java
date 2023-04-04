package com.champtitles.metabasereportexecutor.notifier;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.champtitles.metabasereportexecutor.executor.KmsDecrypt;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class App implements RequestHandler<SNSEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final String awsRegion = System.getenv("AWS_REGION");
    private static final String bucket = System.getenv("BUCKET");
    private static final String smtpHost = System.getenv("SMTP_HOST");
    private static final String smtpPort = System.getenv("SMTP_PORT");
    private static final String smtpUser = System.getenv("SMTP_USER");
    private static final String smtpPasswordKms = System.getenv("SMTP_PASSWORD_KMS");
    private static final String fromAddress = System.getenv("FROM_ADDRESS");
    private static final String recipients = System.getenv("RECIPIENTS");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonPointer objectKeyPtr = JsonPointer.compile("/Records/0/s3/object/key");
    private final S3Reader s3Reader;
    private final EmailSender emailSender;

    public App() {
        KmsDecrypt kmsDecrypt = new KmsDecrypt(awsRegion);
        s3Reader = new S3Reader(bucket);
        emailSender = new EmailSender(smtpHost, smtpPort, smtpUser, kmsDecrypt.decrypt(smtpPasswordKms), fromAddress);
    }

    @Override
    public Void handleRequest(SNSEvent snsEvent, Context context) {

        for (SNSEvent.SNSRecord snsRecord : snsEvent.getRecords()) {
            String s3Key = parseS3Key(snsRecord.getSNS().getMessage());
            byte[] data = s3Reader.downloadXlsx(s3Key);
            logger.info("downloaded {} bytes", data.length);
            emailSender.sendEmail("metabase report", recipients.split(","), getS3FileName(s3Key), data);
        }

        return null;
    }

    /**
     * Parse and return the S3 key from an SNS message body
     *
     * @param snsMessage body of SNS message containing an S3 event
     * @return string of S3 key
     */
    static String parseS3Key(String snsMessage) {
        try {
            logger.info("parsing s3 key from sns message: {}", snsMessage);
            JsonNode root = mapper.readTree(snsMessage);
            return root.at(objectKeyPtr).textValue();

        } catch (JsonProcessingException e) {
            logger.error("failed to parse SNS message: {}", snsMessage);
            throw new NoSuchElementException(e.getMessage());
        }
    }

    /**
     * Parse an S3 key string into just the file name
     *
     * @param s3Key full S3 path (ex: /one/two/foo.txt)
     * @return file name with extension (ex: foo.txt)
     */
    static String getS3FileName(String s3Key) {
        String[] parts = s3Key.split("/");
        return parts[parts.length - 1];
    }
}
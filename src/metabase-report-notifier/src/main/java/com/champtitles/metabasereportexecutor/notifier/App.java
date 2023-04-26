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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

public class App implements RequestHandler<SNSEvent, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class.getName());
    private static final String AWS_REGION = System.getenv("AWS_REGION");
    private static final String BUCKET = System.getenv("BUCKET");
    private static final String SMTP_HOST = System.getenv("SMTP_HOST");
    private static final String SMTP_PORT = System.getenv("SMTP_PORT");
    private static final String SMTP_USER = System.getenv("SMTP_USER");
    private static final String SMTP_PASSWORD_KMS = System.getenv("SMTP_PASSWORD_KMS");
    private static final String FROM_ADDRESS = System.getenv("FROM_ADDRESS");
    private static final String RECIPIENTS = System.getenv().getOrDefault("RECIPIENTS", "test@example.com");
    private static final String METABASE_CARD_ID = System.getenv().getOrDefault("METABASE_CARD_ID", "1");
    private static final String NAME = System.getenv().getOrDefault("NAME", "Test");
    private static final boolean INCLUDE_CARD_IN_SUBJECT = Boolean.parseBoolean(System.getenv().getOrDefault("INCLUDE_CARD_IN_SUBJECT", "false"));
    private static final String SIZE_LIMIT_BYTES = System.getenv().getOrDefault("SIZE_LIMIT_BYTES", "26214400");
    private static final String BODY = System.getenv().getOrDefault("BODY", "");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonPointer OBJECT_KEY_PTR = JsonPointer.compile("/Records/0/s3/object/key");
    private final S3Reader s3Reader;
    private final EmailSender emailSender;

    public App() {
        this(new S3Reader(BUCKET), new EmailSender(SMTP_HOST, SMTP_PORT, SMTP_USER, new KmsDecrypt((AWS_REGION)).decrypt(SMTP_PASSWORD_KMS), FROM_ADDRESS));
    }

    App(S3Reader s3Reader, EmailSender emailSender) {
        this.s3Reader = s3Reader;
        this.emailSender = emailSender;
    }

    @Override
    public Void handleRequest(SNSEvent snsEvent, Context context) {

        for (SNSEvent.SNSRecord snsRecord : snsEvent.getRecords()) {
            String s3Key = parseS3Key(snsRecord.getSNS().getMessage());
            byte[] data = s3Reader.downloadXlsx(s3Key);
            LOGGER.info("downloaded {} bytes for s3 file: {}", data.length, s3Key);
            checkFileSize(data.length);
            emailSender.sendEmail(createSubject(METABASE_CARD_ID, NAME, INCLUDE_CARD_IN_SUBJECT), RECIPIENTS.split(","), createHtmlBody(BODY), getS3FileName(s3Key), data);
        }

        return null;
    }

    /**
     * Parse and return the S3 key from an SNS message body
     *
     * @param snsMessage body of SNS message containing an S3 event
     * @return string of S3 key
     */
    private static String parseS3Key(String snsMessage) {
        try {
            LOGGER.info("parsing s3 key from sns message: {}", snsMessage);
            JsonNode root = OBJECT_MAPPER.readTree(snsMessage);
            return root.at(OBJECT_KEY_PTR).textValue();

        } catch (JsonProcessingException e) {
            LOGGER.error("failed to parse SNS message: {}", snsMessage);
            throw new NoSuchElementException(e.getMessage());
        }
    }

    /**
     * Parse an S3 key string into just the file name
     *
     * @param s3Key full S3 path (ex: /one/two/foo.txt)
     * @return file name with extension (ex: foo.txt)
     */
    private static String getS3FileName(String s3Key) {
        String[] parts = s3Key.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Validate the size of the file is not too large and not empty
     *
     * @param byteLength length of the file in bytes
     */
    private static void checkFileSize(int byteLength) {
        if (byteLength <= 0) {
            throw new RuntimeException("not processing empty file");
        } else if (byteLength > Integer.parseInt(SIZE_LIMIT_BYTES)) {
            throw new RuntimeException("file is greater than max allowed size");
        }
    }

    /**
     * Create an email subject line with the month and year included
     *
     * @param cardId id of the metabase card
     * @param name   name for the report
     * @return formatted subject line
     */
    private static String createSubject(String cardId, String name, boolean includeCardId) {
        LocalDateTime now = LocalDateTime.now();
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));

        String card = "";
        if (includeCardId) {
            card = cardId + ": ";
        }

        return card + name + " For " + month + " " + year;
    }

    /**
     * Create an email HTML body from the provided body
     *
     * @param body content for the email body
     * @return html body
     */
    private String createHtmlBody(String body) {
        String htmlBody = body;

        if (!htmlBody.startsWith("<html>")) {
            htmlBody = "<html>" + htmlBody;
        }
        if (!htmlBody.endsWith("</html>")) {
            htmlBody = htmlBody + "</html>";
        }

        return htmlBody;
    }
}
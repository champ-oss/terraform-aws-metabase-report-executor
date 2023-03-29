package com.champtitles.metabasereportexecutor.executor;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;

public class KmsDecrypt {

    private final KmsClient kmsClient;

    public KmsDecrypt(String region) {
        kmsClient = KmsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String decrypt(String ciphertextBlob) {
        DecryptRequest decryptRequest = DecryptRequest.builder().ciphertextBlob(SdkBytes.fromUtf8String(ciphertextBlob)).build();
        DecryptResponse decryptResponse = kmsClient.decrypt(decryptRequest);
        return decryptResponse.plaintext().toString();
    }

}

package by.niruin.techprocess_service.file_service.configuration;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketLifecycleArgs;
import io.minio.Time;
import io.minio.errors.MinioException;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfiguration {
    private static final Logger log = LogManager.getLogger(MinioConfiguration.class);

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        var minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getUser(), properties.getPassword())
                .build();

        try {
            createPermanentFilesBucket(minioClient, properties);
            createTempFilesBucket(minioClient, properties);

            addAutocleaningRuleToTempBucket(minioClient, properties);

            return minioClient;
        } catch (Exception e) {
            log.fatal("MinIO initialization error: {}", e.getMessage(), e);

            throw new RuntimeException("MinIO initialization error");
        }
    }

    private void addAutocleaningRuleToTempBucket(MinioClient minioClient, MinioProperties properties) throws MinioException {
        var autocleaningRule = createAutocleaningRule(properties);
        minioClient.setBucketLifecycle(
                SetBucketLifecycleArgs.builder()
                        .bucket(properties.getTemporaryFilesBucketName())
                        .config(autocleaningRule)
                        .build()
        );
    }

    private void createPermanentFilesBucket(MinioClient client, MinioProperties properties) throws MinioException {
        if (isBucketNotExist(client, properties.getPermanentFileBucketName())) {
            client.makeBucket(
                    MakeBucketArgs
                            .builder()
                            .bucket(properties.getPermanentFileBucketName())
                            .build());
        }
    }

    private void createTempFilesBucket(MinioClient client, MinioProperties properties) throws MinioException {
        if (isBucketNotExist(client, properties.getTemporaryFilesBucketName())) {
            client.makeBucket(
                    MakeBucketArgs
                            .builder()
                            .bucket(properties.getTemporaryFilesBucketName())
                            .build());
        }
    }

    private boolean isBucketNotExist(MinioClient client, String bucketName) throws MinioException {
        return !client.bucketExists(
                io.minio.BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
    }

    private LifecycleConfiguration createAutocleaningRule(MinioProperties properties) {
        var lifetimeDays = properties.getTemporaryFilesLifeDays();

        if (lifetimeDays == null || lifetimeDays <= 0) {
            log.warn("Temporary files lifetime not configured, skipping lifecycle setup");

            throw new RuntimeException("MinIO initialization error");
        }

        LifecycleConfiguration.Expiration expiration = new LifecycleConfiguration.Expiration(
                (Time.S3Time) null,
                lifetimeDays,
                null,
                null);

        var rule = new LifecycleConfiguration.Rule(
                Status.ENABLED,
                null,
                expiration,
                null,
                "temp-files-auto-cleanup",
                null,
                null,
                null);

        return new LifecycleConfiguration(List.of(rule));
    }
}

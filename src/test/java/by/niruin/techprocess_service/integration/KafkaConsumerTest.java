package by.niruin.techprocess_service.integration;

import by.niruin.techprocess_service.configuration.KafkaConfig;
import by.niruin.techprocess_service.file_service.configuration.MinioProperties;
import by.niruin.techprocess_service.file_service.model.event.EventType;
import by.niruin.techprocess_service.file_service.model.event.FileDeletedEvent;
import by.niruin.techprocess_service.file_service.model.event.MoveFileToPermanentStorageEvent;
import by.niruin.techprocess_service.file_service.service.FileImageService;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import({KafkaConfig.class})
public class KafkaConsumerTest extends BaseIntegrationTest {
    @Value("${spring.kafka.topic-name}")
    private String topicName;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private KafkaContainer kafkaContainer;
    @Autowired
    private FileImageService fileImageService;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        clearBucket(properties.getTemporaryFilesBucketName());
        clearBucket(properties.getPermanentFileBucketName());
    }

    @Test
    void handleMoveEvent_shouldTransferFile_whenValidEventReceived() {
        var fileName = uploadFileToTemp("test-content".getBytes());
        var event = new MoveFileToPermanentStorageEvent(fileName, EventType.FILE_MOVE_TO_PERMANENT_STORAGE.name());
        kafkaTemplate.send(topicName, event);

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> isFileExistsInPermanent(fileName));
        assertThat(isFileExistsInPermanent(fileName)).isTrue();
        assertThat(isFileExistsInTemp(fileName)).isFalse();
    }

    @Test
    void shouldNotTransferFile_whenFileNameIsNull() {
        var event = new MoveFileToPermanentStorageEvent(null, EventType.FILE_MOVE_TO_PERMANENT_STORAGE.name());
        kafkaTemplate.send(topicName, event);

        await().atMost(Duration.ofSeconds(5))
                .pollDelay(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(getAllFilesInBucket(properties.getPermanentFileBucketName())).isEmpty());
    }

    @Test
    void shouldNotTransferFile_whenFileNameIsBlank() {
        var event = new MoveFileToPermanentStorageEvent("", EventType.FILE_MOVE_TO_PERMANENT_STORAGE.name());
        kafkaTemplate.send(topicName, event);

        await().atMost(Duration.ofSeconds(5))
                .pollDelay(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(getAllFilesInBucket(properties.getPermanentFileBucketName())).isEmpty());
    }

    @Test
    void shouldHandleNonExistentFile_whenMoveEventReceived() {
        var nonExistentFile = UUID.randomUUID() + ".png";
        var event = new MoveFileToPermanentStorageEvent(nonExistentFile, EventType.FILE_MOVE_TO_PERMANENT_STORAGE.name());
        kafkaTemplate.send(topicName, event);

        await().atMost(Duration.ofSeconds(5))
                .pollDelay(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(isFileExistsInPermanent(nonExistentFile)).isFalse());
    }

    @Test
    void shouldNotDeleteFile_whenFileNameIsNull() {
        var fileName = uploadFileToTemp("test-content".getBytes());
        fileImageService.transferToPermanentBucket(fileName);
        var event = new FileDeletedEvent(null, EventType.FILE_DELETED_EVENT.name());
        kafkaTemplate.send(topicName, event);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(isFileExistsInPermanent(fileName)).isTrue());
    }

    @Test
    void shouldNotDeleteFile_whenFileNameIsBlank() throws Exception {
        var fileName = uploadFileToTemp("test-content".getBytes());
        fileImageService.transferToPermanentBucket(fileName);
        var event = new FileDeletedEvent("", EventType.FILE_DELETED_EVENT.name());
        kafkaTemplate.send(topicName, event);

        await().atMost(Duration.ofSeconds(5))
                .pollDelay(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(isFileExistsInPermanent(fileName)).isTrue());
    }

    @Test
    void shouldHandleNonExistentFile_whenDeleteEventReceived() {
        var nonExistentFile = UUID.randomUUID() + ".png";
        var event = new FileDeletedEvent(nonExistentFile, EventType.FILE_DELETED_EVENT.name());
        kafkaTemplate.send(topicName, event);

        await().atMost(Duration.ofSeconds(5))
                .pollDelay(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(isFileExistsInPermanent(nonExistentFile)).isFalse());
    }

    @Test
    void shouldHandleUnknownEventType() {
        var fileName = uploadFileToTemp("test-content".getBytes());
        kafkaTemplate.send(topicName, "unknownEvent");

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(isFileExistsInTemp(fileName)).isTrue();
                    assertThat(isFileExistsInPermanent(fileName)).isFalse();
                });
    }

    private String uploadFileToTemp(byte[] content) {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", content);
        return fileImageService.upload(file);
    }

    private boolean isFileExistsInTemp(String fileName) {
        return fileImageService.isFileExist(fileName, properties.getTemporaryFilesBucketName());
    }

    private boolean isFileExistsInPermanent(String fileName) {
        return fileImageService.isFileExist(fileName, properties.getPermanentFileBucketName());
    }

    private List<String> getAllFilesInBucket(String bucketName) throws MinioException {
        List<String> files = new java.util.ArrayList<>();
        var results = minioClient.listObjects(
                io.minio.ListObjectsArgs.builder().bucket(bucketName).build()
        );
        for (var result : results) {
            files.add(result.get().objectName());
        }
        return files;
    }

    private void clearBucket(String bucketName) throws Exception {
        var results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).build()
        );

        for (var result : results) {
            var item = result.get();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(item.objectName())
                            .build()
            );
        }
    }
}
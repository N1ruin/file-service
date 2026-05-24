package by.niruin.techprocess_service.file_service.kafka;

import by.niruin.techprocess_service.file_service.model.event.FileDeletedEvent;
import by.niruin.techprocess_service.file_service.model.event.MoveFileToPermanentStorageEvent;
import by.niruin.techprocess_service.file_service.service.FileImageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${spring.kafka.topic-name}", groupId = "${spring.kafka.group-id}")
public class KafkaConsumer {
    private static final Logger logger = LogManager.getLogger(KafkaConsumer.class);
    private final FileImageService fileImageService;

    public KafkaConsumer(FileImageService fileImageService) {
        this.fileImageService = fileImageService;
    }

    @KafkaHandler
    public void handleMoveEvent(MoveFileToPermanentStorageEvent event) {
        if (isFileNameIsNullOrBlank(event.fileName())) {
            return;
        }

        logger.info("Transferring file '{}' from temp to permanent bucket", event.fileName());

        try {
            fileImageService.transferToPermanentBucket(event.fileName());

            logger.info("File '{}' successfully transferred", event.fileName());
        } catch (Exception e) {
            logger.error("Move file to permanent storage event processing", e);
        }

    }

    @KafkaHandler
    public void handleDeleteEvent(FileDeletedEvent event) {
        if (isFileNameIsNullOrBlank(event.fileName())) {
            return;
        }

        logger.info("Deleting file '{}' from permanent bucket", event.fileName());

        try {
            fileImageService.delete(event.fileName());

            logger.info("File '{}' successfully deleted", event.fileName());
        } catch (Exception e) {
            logger.error("Move file to permanent storage event processing", e);
        }
    }

    private boolean isFileNameIsNullOrBlank(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("Received event with null or blank fileName");
            return true;
        }

        return false;
    }
}

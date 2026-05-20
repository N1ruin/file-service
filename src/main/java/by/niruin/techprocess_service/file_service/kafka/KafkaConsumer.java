package by.niruin.techprocess_service.file_service.kafka;

import by.niruin.techprocess_service.file_service.exception.TransferFileException;
import by.niruin.techprocess_service.file_service.model.event.EquipmentSaveSuccessEvent;
import by.niruin.techprocess_service.file_service.service.FileImageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {
    private static final Logger logger = LogManager.getLogger(KafkaConsumer.class);
    private final FileImageService fileImageService;

    public KafkaConsumer(FileImageService fileImageService) {
        this.fileImageService = fileImageService;
    }

    @KafkaListener(topics = "file-topic", groupId = "file-service-group")
    public void listen(EquipmentSaveSuccessEvent event) {
        if (event == null || event.fileName() == null) {
            logger.warn("Received null event or fileName is null");

            return;
        }

        try {
            logger.info("Transferring file '{}' from temp to permanent bucket", event.fileName());

            fileImageService.transferToPermanentBucket(event.fileName());

            logger.info("File '{}' successfully transferred", event.fileName());
        } catch (Exception e) {
            logger.error("Failed to transfer file '{}': {}", event.fileName(), e.getMessage(), e);

            throw new TransferFileException("Failed to transfer file: " + event.fileName(), e);
        }
    }
}

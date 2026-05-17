package by.niruin.techprocess_service.file_service.kafka;

import by.niruin.techprocess_service.file_service.model.event.ImageDeletedEvent;
import by.niruin.techprocess_service.file_service.service.FileImageService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaFileConsumer {
    private final FileImageService fileImageService;

    public KafkaFileConsumer(FileImageService fileImageService) {
        this.fileImageService = fileImageService;
    }

    @KafkaListener(
            topics = "file-deletion-topic",
            groupId = "file-service-group")
    public void listen(ImageDeletedEvent event) {
        if (event == null || event.fileName() == null) {
            return;
        }

        fileImageService.delete(event.fileName());
    }
}

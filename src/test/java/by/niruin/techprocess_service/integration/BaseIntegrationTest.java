package by.niruin.techprocess_service.integration;

import by.niruin.techprocess_service.configuration.KafkaConfig;
import by.niruin.techprocess_service.configuration.MinioConfig;
import by.niruin.techprocess_service.file_service.FileServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = FileServiceApplication.class)
@Import({MinioConfig.class, KafkaConfig.class})
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BaseIntegrationTest {
}

package by.niruin.techprocess_service.file_service.model;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record UpdateFileRequest(@NotNull MultipartFile file) {
}

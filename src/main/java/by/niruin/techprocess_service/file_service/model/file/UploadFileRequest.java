package by.niruin.techprocess_service.file_service.model.file;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record UploadFileRequest(@NotNull MultipartFile file) {
}

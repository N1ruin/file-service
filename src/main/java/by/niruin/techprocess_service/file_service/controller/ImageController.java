package by.niruin.techprocess_service.file_service.controller;

import by.niruin.techprocess_service.file_service.model.file.UpdateFileRequest;
import by.niruin.techprocess_service.file_service.model.file.UpdateFileResponse;
import by.niruin.techprocess_service.file_service.model.file.UploadFileRequest;
import by.niruin.techprocess_service.file_service.model.file.UploadFileResponse;
import by.niruin.techprocess_service.file_service.service.FileImageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static by.niruin.techprocess_service.file_service.constant.Regex.FILE_NAME_REGEX;

@RestController
@RequestMapping("/api/v1/file-service/files/images")
public class ImageController {
    private final FileImageService fileImageService;

    public ImageController(FileImageService fileImageService) {
        this.fileImageService = fileImageService;
    }

    @PostMapping
    public ResponseEntity<UploadFileResponse> upload(@Valid @ModelAttribute UploadFileRequest request) {
        var file = request.file();

        var newFileName = fileImageService.upload(file);

        var response = new UploadFileResponse(newFileName);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping(value = "/{file-name}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable("file-name") @NotNull
                                           @Pattern(regexp = FILE_NAME_REGEX) String fileName) {
        var fileBytes = fileImageService.download(fileName);

        return ResponseEntity.ok(fileBytes);
    }

    @PutMapping(value = "/{file-name}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UpdateFileResponse> update(@PathVariable("file-name") @NotNull @Pattern(regexp = FILE_NAME_REGEX)
                                                     String fileName,
                                                     @Valid @ModelAttribute UpdateFileRequest request) {
        var updatedFileName = fileImageService.update(fileName, request.file());

        var response = new UpdateFileResponse(updatedFileName);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{file-name}")
    public ResponseEntity<Void> delete(@PathVariable("file-name")
                                       @NotNull @Pattern(regexp = FILE_NAME_REGEX) String fileName) {
        fileImageService.delete(fileName);

        return ResponseEntity.noContent()
                .build();
    }
}

package by.niruin.techprocess_service.file_service.service;

import by.niruin.techprocess_service.file_service.configuration.MinioProperties;
import by.niruin.techprocess_service.file_service.exception.*;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;


@Service
public class FileImageService {
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png");
    private final MinioClient minioClient;
    private final MinioProperties properties;


    public FileImageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public String upload(MultipartFile file) {
        checkFileSize(file);

        var originalFileName = file.getOriginalFilename();
        var extension = extractFileExtension(originalFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileFormatException("Invalid image %s format".formatted(originalFileName));
        }

        var newFileName = UUID.randomUUID() + extension;
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(newFileName)
                            .stream(file.getInputStream(), file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new FileUploadException("File %s upload error".formatted(originalFileName), e);
        }

        return newFileName;
    }

    public byte[] download(String fileName) {
        checkFileExistence(fileName);

        try {
            var response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(fileName)
                            .build()
            );

            return response.readAllBytes();
        } catch (Exception e) {
            throw new FileDownloadException("File %s download error".formatted(fileName), e);
        }
    }

    public String update(String fileName, MultipartFile file) {
        checkFileExistence(fileName);
        checkFileSize(file);

        var extension = extractFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileFormatException("Invalid image format: " + file.getOriginalFilename());
        }

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new FileUpdateException("Update file %s error".formatted(fileName), e);
        }

        return fileName;
    }

    public void delete(String fileName) {
        checkFileExistence(fileName);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new FileDeleteException("Failed to delete file %s from MinIO".formatted(fileName));
        }
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            throw new InvalidFileFormatException("Invalid file %s format exception".formatted(fileName));

        }

        return fileName.substring(fileName.lastIndexOf('.'));
    }

    private void checkFileSize(MultipartFile file) {
        if (file.getSize() > 2097152) {
            throw new FileTooLargeException("The file size cannot exceed 2MB");
        }
    }

    private void checkFileExistence(String fileName) {
        if (!isFileExist(fileName)) {
            throw new FileNotFoundException("File with name %s not found".formatted(fileName));
        }
    }

    private boolean isFileExist(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(fileName)
                            .build());

            return true;
        } catch (ErrorResponseException e) {
            var errorResponse = e.errorResponse();

            if (errorResponse != null && "NoSuchKey".equals(errorResponse.code())) {
                return false;
            }

            var errorCode = errorResponse != null ? errorResponse.code() : "unknown";

            throw new RuntimeException("MinIO error when validating a file " + errorCode, e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error when checking the existence of a file: " + fileName, e);
        }
    }
}

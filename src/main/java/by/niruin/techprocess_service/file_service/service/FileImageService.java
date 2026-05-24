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
                            .bucket(properties.getTemporaryFilesBucketName())
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
        checkFileExistence(fileName, properties.getPermanentFileBucketName());

        try {
            var response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getPermanentFileBucketName())
                            .object(fileName)
                            .build()
            );

            return response.readAllBytes();
        } catch (Exception e) {
            throw new FileDownloadException("File %s download error".formatted(fileName), e);
        }
    }

    public void transferToPermanentBucket(String fileName) {
        checkFileExistence(fileName, properties.getTemporaryFilesBucketName());
        checkFileNotExist(fileName, properties.getPermanentFileBucketName());

        try {

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(properties.getPermanentFileBucketName())
                            .object(fileName)
                            .source(
                                    SourceObject.builder()
                                            .bucket(properties.getTemporaryFilesBucketName())
                                            .object(fileName)
                                            .build()).
                            build());

            deleteFromTemporaryBucket(fileName);
        } catch (Exception e) {
            throw new TransferFileException("File %s copy error".formatted(fileName), e);
        }
    }

    public void delete(String fileName) {
        checkFileExistence(fileName, properties.getPermanentFileBucketName());

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getPermanentFileBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new FileDeleteException("Failed to delete file %s from MinIO".formatted(fileName));
        }
    }

    public void deleteFromTemporaryBucket(String fileName) {
        checkFileExistence(fileName, properties.getTemporaryFilesBucketName());

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getTemporaryFilesBucketName())
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

    private void checkFileExistence(String fileName, String bucketName) {
        if (!isFileExist(fileName, bucketName)) {
            throw new FileNotFoundException("File with name %s not found".formatted(fileName));
        }
    }

    private void checkFileNotExist(String fileName, String bucketName) {
        if (isFileExist(fileName, bucketName)) {
            throw new FileAlreadyExistException("File with name %s already exists in permanent storage".formatted(fileName));
        }
    }

    public boolean isFileExist(String fileName, String bucketName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
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

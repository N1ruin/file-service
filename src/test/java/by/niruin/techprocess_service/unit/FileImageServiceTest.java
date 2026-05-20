package by.niruin.techprocess_service.unit;

import by.niruin.techprocess_service.file_service.configuration.MinioProperties;
import by.niruin.techprocess_service.file_service.exception.FileNotFoundException;
import by.niruin.techprocess_service.file_service.exception.FileTooLargeException;
import by.niruin.techprocess_service.file_service.exception.InvalidFileFormatException;
import by.niruin.techprocess_service.file_service.service.FileImageService;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import static by.niruin.techprocess_service.file_service.constant.Regex.FILE_NAME_REGEX;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileImageServiceTest {
    @Mock
    private MinioClient minioClient;
    @Mock
    private MinioProperties minioProperties;
    @InjectMocks
    private FileImageService fileImageService;

    @Test
    void upload_shouldThrowsTooLargeException_whereFileSizeMore2MB() throws MinioException {
        var file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(3 * 1024 * 1024L);

        assertThatThrownBy(() -> fileImageService.upload(file))
                .isInstanceOf(FileTooLargeException.class);

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    void upload_shouldNotThrowException_whenFileSizeIsExactly2MB() throws Exception {
        byte[] fileContent = "test image content".getBytes();
        var file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(2 * 1024 * 1024L);
        when(file.getOriginalFilename()).thenReturn("image.jpg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(file.getContentType()).thenReturn("image/jpeg");
        when(minioProperties.getPermanentFileBucketName()).thenReturn("test-bucket");
        var mockResponse = mock(ObjectWriteResponse.class);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mockResponse);

        assertDoesNotThrow(() -> fileImageService.upload(file));

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {".txt", ".exe", ".gif", ".bmp"})
    void upload_shouldThrowsInvalidFileFormat_whereFileExtensionIsXls(String extension) throws MinioException {
        var file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("testfile" + extension);

        assertThatThrownBy(() -> fileImageService.upload(file))
                .isInstanceOf(InvalidFileFormatException.class);

        verify(minioClient, never()).putObject(any());
    }

    @Test
    void upload_shouldReturnNewFileName_whereFileIsValid() throws IOException, MinioException {
        byte[] fileContent = "test image content".getBytes();
        var file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn((long) fileContent.length);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));
        when(minioProperties.getPermanentFileBucketName()).thenReturn("test-bucket");
        var mockResponse = mock(ObjectWriteResponse.class);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mockResponse);

        var result = fileImageService.upload(file);

        assertThat(result).isNotNull();
        assertThat(result).matches(FILE_NAME_REGEX);
        verify(minioClient).putObject(any(PutObjectArgs.class));
        verifyNoMoreInteractions(minioClient);
    }

    @Test
    void download_shouldReturnBytes_whereFileExist() throws MinioException, IOException {
        var fileName = UUID.randomUUID() + ".jpeg";
        byte[] expectedContent = "test image content".getBytes();
        when(minioProperties.getPermanentFileBucketName()).thenReturn("test-bucket");
        var mockResponse = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);
        when(mockResponse.readAllBytes()).thenReturn(expectedContent);

        var content = fileImageService.download(fileName);

        assertThat(expectedContent).isEqualTo(content);
    }

    @Test
    void download_shouldThrowFileNotFound() throws MinioException {
        var fileName = UUID.randomUUID() + ".jpeg";
        when(minioProperties.getPermanentFileBucketName()).thenReturn("test-bucket");
        var mockErrorResponseException = mock(ErrorResponseException.class);
        var mockErrorResponse = mock(ErrorResponse.class);
        when(mockErrorResponseException.errorResponse()).thenReturn(mockErrorResponse);
        when(mockErrorResponse.code()).thenReturn("NoSuchKey");
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(mockErrorResponseException);

        assertThatThrownBy(() -> fileImageService.download(fileName))
                .isInstanceOf(FileNotFoundException.class);

        verify(minioClient).statObject(any(StatObjectArgs.class));
        verify(minioClient, never()).getObject(any(GetObjectArgs.class));
    }

    @Test
    void delete_shouldThrowFileNotFound() throws MinioException {
        var fileName = UUID.randomUUID() + ".jpeg";
        when(minioProperties.getPermanentFileBucketName()).thenReturn("test-bucket");
        var mockErrorResponseException = mock(ErrorResponseException.class);
        var mockErrorResponse = mock(ErrorResponse.class);
        when(mockErrorResponseException.errorResponse()).thenReturn(mockErrorResponse);
        when(mockErrorResponse.code()).thenReturn("NoSuchKey");
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(mockErrorResponseException);

        assertThatThrownBy(() -> fileImageService.delete(fileName))
                .isInstanceOf(FileNotFoundException.class);

        verify(minioClient).statObject(any(StatObjectArgs.class));
        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteSuccess_whereFileExist() throws MinioException {
        var fileName = UUID.randomUUID() + ".jpeg";
        when(minioProperties.getPermanentFileBucketName()).thenReturn("test-bucket");
        var statObjectResponse = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);

        assertDoesNotThrow(() -> fileImageService.delete(fileName));
    }
}

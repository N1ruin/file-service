package by.niruin.techprocess_service.integration;

import by.niruin.techprocess_service.file_service.configuration.MinioProperties;
import by.niruin.techprocess_service.file_service.service.FileImageService;
import com.jayway.jsonpath.JsonPath;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static by.niruin.techprocess_service.file_service.constant.Regex.FILE_NAME_REGEX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FileImageServiceIT extends BaseIntegrationTest {
    private static final String BASE_REQUEST_PATH = "/api/v1/file-service/files/images";
    @Autowired
    private MinIOContainer minIOContainer;
    @Autowired
    private FileImageService fileImageService;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioProperties properties;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clearMinIO() throws MinioException {
        var results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(properties.getPermanentFileBucketName())
                        .build()
        );

        for (var result : results) {
            var item = result.get();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getPermanentFileBucketName())
                            .object(item.objectName())
                            .build()
            );
        }
    }

    @Test
    void upload_shouldReturnSavedFileName() throws Exception {
        var fileContent = "test-content".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "testimagename.jpg",
                MediaType.IMAGE_JPEG.toString(), fileContent);

        var result = mockMvc.perform(multipart(HttpMethod.POST, BASE_REQUEST_PATH)
                        .file(file)
                        .with(jwt()))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.fileName").exists(),
                        jsonPath("$.fileName").exists())
                .andReturn();

        var json = result.getResponse().getContentAsString();
        String fileName = JsonPath.read(json, "$.fileName");

        assertThat(fileName).matches(FILE_NAME_REGEX);
    }

    @Test
    void upload_shouldThrowFileTooLargeException_whereFileSizeMore2MB_andFileNameValid() throws Exception {
        var largeData = new byte[3 * 1024 * 1024];
        var file = new MockMultipartFile("file", "testimagename.jpg",
                MediaType.IMAGE_JPEG.toString(), largeData);

        mockMvc.perform(multipart(HttpMethod.POST, BASE_REQUEST_PATH)
                        .file(file)
                        .with(jwt()))
                .andExpectAll(
                        status().isContentTooLarge(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(413));
    }

    @Test
    void upload_shouldThrowInvalidFileFormat_andFileSizeValid() throws Exception {
        var fileContent = "test-content".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "testimagename.docx",
                MediaType.IMAGE_JPEG.toString(), fileContent);

        mockMvc.perform(multipart(HttpMethod.POST, BASE_REQUEST_PATH)
                        .file(file)
                        .with(jwt()))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));
    }

    @Test
    void download_shouldReturnSavedFile() throws Exception {
        var fileContent = "test-content".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "testimagename.png",
                MediaType.IMAGE_JPEG.toString(), fileContent);

        var savedFileName = fileImageService.upload(file);
        fileImageService.transferToPermanentBucket(savedFileName);

        var result = mockMvc.perform(get(BASE_REQUEST_PATH + "/{file-name}", savedFileName)
                        .with(jwt()))
                .andExpectAll(
                        status().isOk())
                .andReturn();

        byte[] actualBytes = result.getResponse().getContentAsByteArray();
        assertThat(actualBytes).isEqualTo(file.getBytes());
    }

    @Test
    void download_shouldThrowFileNotFound() throws Exception {
        var fileName = UUID.randomUUID() + ".png";

        mockMvc.perform(get(BASE_REQUEST_PATH + "/{file-name}", fileName)
                        .with(jwt()))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));
    }

    @Test
    void download_shouldThrowInvalidFileFormat() throws Exception {
        var fileName = UUID.randomUUID() + ".docx";

        mockMvc.perform(get(BASE_REQUEST_PATH + "/{file-name}", fileName)
                        .with(jwt()))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));
    }

    @Test
    void deleteSuccess() throws Exception {
        var fileContent = "test-content".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "testimagename.png",
                MediaType.IMAGE_JPEG.toString(), fileContent);

        var savedFileName = fileImageService.upload(file);
        fileImageService.transferToPermanentBucket(savedFileName);

        mockMvc.perform(delete(BASE_REQUEST_PATH + "/{file-name}", savedFileName)
                        .with(jwt()))
                .andExpectAll(
                        status().isNoContent());
    }

    @Test
    void delete_shouldThrowFileNotFound() throws Exception {
        var fileName = UUID.randomUUID() + ".png";

        mockMvc.perform(delete(BASE_REQUEST_PATH + "/{file-name}", fileName)
                        .with(jwt()))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(404));
    }

    @Test
    void delete_shouldThrowParameterValidationEx_whereInvalidFileFormat() throws Exception {
        var fileName = UUID.randomUUID() + ".xls";

        mockMvc.perform(delete(BASE_REQUEST_PATH + "/{file-name}", fileName)
                        .with(jwt()))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(400));
    }
}

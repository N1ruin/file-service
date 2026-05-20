package by.niruin.techprocess_service.file_service.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint;
    private String user;
    private String password;
    private String permanentFileBucketName;
    private String temporaryFilesBucketName;
    private Long maxFileSize;
    private Integer temporaryFilesLifeDays;

    public String getEndpoint() {
        return endpoint;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getPermanentFileBucketName() {
        return permanentFileBucketName;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public String getTemporaryFilesBucketName() {
        return temporaryFilesBucketName;
    }

    public Integer getTemporaryFilesLifeDays() {
        return temporaryFilesLifeDays;
    }
}
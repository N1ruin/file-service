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

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPermanentFileBucketName() {
        return permanentFileBucketName;
    }

    public void setPermanentFileBucketName(String permanentFileBucketName) {
        this.permanentFileBucketName = permanentFileBucketName;
    }

    public String getTemporaryFilesBucketName() {
        return temporaryFilesBucketName;
    }

    public void setTemporaryFilesBucketName(String temporaryFilesBucketName) {
        this.temporaryFilesBucketName = temporaryFilesBucketName;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public Integer getTemporaryFilesLifeDays() {
        return temporaryFilesLifeDays;
    }

    public void setTemporaryFilesLifeDays(Integer temporaryFilesLifeDays) {
        this.temporaryFilesLifeDays = temporaryFilesLifeDays;
    }
}

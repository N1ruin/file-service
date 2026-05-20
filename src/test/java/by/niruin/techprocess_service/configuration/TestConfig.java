package by.niruin.techprocess_service.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("test")
public class TestConfig {
    @Bean
    public MinIOContainer minIOContainer() {
        return new MinIOContainer(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z-cpuv1"));
    }

    @Bean
    public DynamicPropertyRegistrar minioPropertiesRegistrar(MinIOContainer minIOContainer) {
        return registry -> {
            registry.add("minio.endpoint", minIOContainer::getS3URL);
            registry.add("minio.user", minIOContainer::getUserName);
            registry.add("minio.password", minIOContainer::getPassword);
            registry.add("minio.bucketName", () -> "equipments");
            registry.add("minio.maxFileSize", () -> 2097152);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http){
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}

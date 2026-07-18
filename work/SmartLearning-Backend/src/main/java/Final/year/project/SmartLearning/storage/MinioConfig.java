package Final.year.project.SmartLearning.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    // Host the BROWSER can reach (used only for presigned URLs).
    // Defaults to the internal url if not separately configured.
    @Value("${minio.public-url:${minio.url}}")
    private String publicUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.region:us-east-1}")
    private String region;

    /** Used for server-to-server calls: upload, delete. Internal Docker hostname is fine here. */
    @Bean
    public MinioClient minioClient() {

        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }
}









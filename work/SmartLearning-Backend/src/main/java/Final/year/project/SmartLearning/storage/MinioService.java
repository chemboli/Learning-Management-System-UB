package Final.year.project.SmartLearning.storage;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucket;

    @Value("${minio.public-url:${minio.url}}")
    private String publicUrl;

    /**
     * Builds a plain, unsigned download URL for an object in the bucket.
     *
     * Deliberately NOT a presigned (signed) URL: the bucket's public-access
     * domain (e.g. a Cloudflare R2 pub-xxxx.r2.dev URL) serves any object in
     * the bucket to anyone with the URL, unsigned — it doesn't understand or
     * check S3 signature query parameters at all. Generating a presigned URL
     * against that host (as this used to do) produces a broken link: a
     * signature the server ignores, plus a duplicated bucket-name path
     * segment that doesn't exist on that host. This also means anyone who
     * has the link can access the file indefinitely — acceptable here since
     * the bucket is intentionally public, but worth knowing if that ever
     * needs to change (in which case, presigned URLs against the real S3 API
     * endpoint — minio.url, not minio.public-url — are the right tool).
     */
    public String buildPublicUrl(String objectName) {
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;

        String encodedPath = java.util.Arrays.stream(objectName.split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .reduce((a, b) -> a + "/" + b)
                .orElse(objectName);

        return base + "/" + encodedPath;
    }

    public String uploadFile(MultipartFile file, String folder) {

        try {
            String objectName =
                    folder + "/" +
                    UUID.randomUUID() + "_" +
                    file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(),
                                    file.getSize(),
                                    -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return objectName;

        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    public void deleteFile(String objectName) {

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("File deletion failed", e);
        }
    }

}
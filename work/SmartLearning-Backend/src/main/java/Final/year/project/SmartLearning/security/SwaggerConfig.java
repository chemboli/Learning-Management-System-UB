package Final.year.project.SmartLearning.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI smartLearningAPI() {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .info(new Info()
                        .title("Smart Learning API")
                        .version("1.0")
                        .description("API documentation for Smart Learning System"));
    }

    /**
     * Force Swagger to treat multipart "request" as JSON object
     */
    @Bean
    public OperationCustomizer multipartNoteUploadCustomizer() {
        return (operation, handlerMethod) -> {

            if (operation.getOperationId() != null
                    && operation.getOperationId().toLowerCase().contains("upload")) {

                MediaType multipartMedia = new MediaType()
                        .schema(new Schema<>()
                                .type("object")
                                .addProperties("file",
                                        new Schema<>()
                                                .type("string")
                                                .format("binary")
                                )
                                .addProperties("request",
                                        new Schema<>()
                                                .$ref("#/components/schemas/CreateNoteRequest")
                                )
                        )
                        .addEncoding("request",
                                new Encoding().contentType("application/json")
                        );

                RequestBody requestBody = new RequestBody()
                        .content(new Content()
                                .addMediaType("multipart/form-data", multipartMedia)
                        );

                operation.setRequestBody(requestBody);
            }

            return operation;
        };
    }
}
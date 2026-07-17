package Final.year.project.SmartLearning.shared;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(ZoneId.systemDefault());

    /**
     * Sends an HTML invitation email to a freshly admin-created account.
     * The mail contains a link to the portal's activation page plus the
     * invitation code the user must enter there to set their password.
     *
     * Rendered from the Thymeleaf template
     * {@code resources/templates/email/invitation-email.html}.
     *
     * @param toEmail        recipient address
     * @param firstName      recipient's first name (used in greeting)
     * @param invitationCode the one-time code to redeem on the portal
     * @param expiresAt      when the code stops working
     */
    public void sendInvitationEmail(String toEmail,
                                    String firstName,
                                    String invitationCode,
                                    Instant expiresAt) {
        try {
            String activationLink = frontendUrl + "/activate?code=" + invitationCode;

            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("invitationCode", invitationCode);
            context.setVariable("activationLink", activationLink);
            context.setVariable("portalUrl", frontendUrl);
            context.setVariable("expiryDate", EXPIRY_FORMAT.format(expiresAt));

            String html = templateEngine.process("email/invitation-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(fromAddress, "SmartLearning");
            helper.setTo(toEmail);
            helper.setSubject("You've been invited to SmartLearning");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Invitation email sent to {}", toEmail);
        } catch (Exception e) {
            // Log but do not propagate — account creation must not fail
            // simply because the mail server is temporarily unavailable.
            // Admins can re-send the invitation from the user list.
            log.error("Failed to send invitation email to {}: {}", toEmail, e.getMessage());
        }
    }
}

package com.hrms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ResendClient resendClient;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String link = frontendUrl + "/onboarding/verify?token=" + token;

        String html = """
                <div style="font-family: -apple-system, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2933;">
                    <h2 style="color: #0f172a;">Welcome aboard, %s</h2>
                    <p>An account has been created for you on the HR Portal.</p>
                    <p>Please verify your account and set up your password using the button below:</p>
                    <p style="margin: 32px 0;">
                        <a href="%s" style="background-color: #0f172a; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; display: inline-block;">
                            Verify Account
                        </a>
                    </p>
                    <p style="font-size: 13px; color: #6b7280;">This link expires in 48 hours. If you did not expect this email, please contact HR immediately.</p>
                </div>
                """.formatted(firstName, link);

        try {
            resendClient.send(toEmail, "Welcome aboard - verify your account", html);
        } catch (ResendClient.EmailDeliveryException e) {
            log.error("Verification email failed to send to {}: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    public void sendLeaveDecisionEmail(String toEmail, String firstName, String decision, String comment) {
        String html = """
                <div style="font-family: -apple-system, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2933;">
                    <h2 style="color: #0f172a;">Hi %s,</h2>
                    <p>Your leave request has been <strong>%s</strong>.</p>
                    %s
                    <p>Log in to the HR Portal for full details.</p>
                </div>
                """.formatted(
                firstName,
                decision.toLowerCase(),
                (comment != null && !comment.isBlank())
                        ? "<p><em>Reviewer comment:</em> " + comment + "</p>"
                        : ""
        );

        try {
            resendClient.send(toEmail, "Your leave request has been " + decision.toLowerCase(), html);
        } catch (ResendClient.EmailDeliveryException e) {
            log.error("Leave decision email failed to send to {}: {}", toEmail, e.getMessage());
        }
    }
}

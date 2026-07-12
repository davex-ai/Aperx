package com.hrms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String firstName, String companyName, String companyEmail, String token) {
        String link = frontendUrl + "/onboarding/verify?token=" + token;

        String html = """
            <div style="font-family: -apple-system, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2933;">
                <h2 style="color: #0f172a;">%s has invited you to join their team on AperX</h2>
                <p>Hi %s,</p>
                <p><strong>%s</strong> uses AperX to manage HR, payroll, and time off. They've created an account for you.</p>
                <p>Your company email is <strong>%s</strong> - use this to sign in going forward.</p>
                <p>Click below to verify your account and set your password:</p>
                <p style="margin: 32px 0;">
                    <a href="%s" style="background-color: #0f172a; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; display: inline-block;">
                        Accept invitation
                    </a>
                </p>
                <p style="font-size: 13px; color: #6b7280;">This link expires in 48 hours. If you weren't expecting this, you can safely ignore this email.</p>
            </div>
            """.formatted(companyName, firstName, companyName, companyEmail, link);

        send(toEmail, companyName + " has invited you to AperX", html);
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
            send(toEmail, "Your leave request has been " + decision.toLowerCase(), html);
        } catch (RuntimeException e) {
            log.error("Leave decision email failed to send to {}: {}", toEmail, e.getMessage());
        }
    }

    private void send(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }
}
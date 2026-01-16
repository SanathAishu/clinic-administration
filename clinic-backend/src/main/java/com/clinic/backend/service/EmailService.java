package com.clinic.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

/**
 * Email Service for sending notifications and alerts.
 * Supports both plain text and HTML emails.
 * Used for SLA violations, compliance alerts, and audit notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@clinic.local}")
    private String fromAddress;

    @Value("${app.admin.email:admin@clinic.local}")
    private String adminEmail;

    @Value("${app.compliance.email:compliance@clinic.local}")
    private String complianceEmail;

    /**
     * Send plain text email to single recipient.
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body (plain text)
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Sent email to {} with subject: {}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Send HTML email to single recipient.
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlBody Email body (HTML formatted)
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(mimeMessage);
            log.info("Sent HTML email to {} with subject: {}", to, subject);

        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Send email to multiple recipients.
     *
     * @param recipients List of email addresses
     * @param subject Email subject
     * @param body Email body
     */
    public void sendEmailToMultiple(List<String> recipients, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Sent email to {} recipients with subject: {}", recipients.size(), subject);

        } catch (Exception e) {
            log.error("Failed to send email to {} recipients: {}", recipients.size(), e.getMessage(), e);
        }
    }

    /**
     * Send HTML email to multiple recipients.
     *
     * @param recipients List of email addresses
     * @param subject Email subject
     * @param htmlBody Email body (HTML formatted)
     */
    public void sendHtmlEmailToMultiple(List<String> recipients, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(mimeMessage);
            log.info("Sent HTML email to {} recipients with subject: {}", recipients.size(), subject);

        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {} recipients: {}", recipients.size(), e.getMessage(), e);
        }
    }

    /**
     * Send email to all administrators.
     * Uses configured admin email list.
     *
     * @param subject Email subject
     * @param htmlBody Email body (HTML formatted)
     */
    public void sendToAdministrators(String subject, String htmlBody) {
        try {
            sendHtmlEmail(adminEmail, subject, htmlBody);
            log.info("Sent admin notification with subject: {}", subject);

        } catch (Exception e) {
            log.error("Failed to send admin notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send email to compliance team.
     * Used for compliance alerts and audit notifications.
     *
     * @param subject Email subject
     * @param htmlBody Email body (HTML formatted)
     */
    public void sendToComplianceTeam(String subject, String htmlBody) {
        try {
            sendHtmlEmail(complianceEmail, subject, htmlBody);
            log.info("Sent compliance notification with subject: {}", subject);

        } catch (Exception e) {
            log.error("Failed to send compliance notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send SLA violation alert email.
     *
     * @param metricType Type of metric that violated SLA
     * @param complianceRate Current compliance rate
     * @param violationCount Number of violations
     */
    public void sendSLAViolationAlert(String metricType, double complianceRate, long violationCount) {
        String subject = String.format("SLA Violation Alert: %s", metricType);

        StringBuilder body = new StringBuilder();
        body.append("<html><body>\n");
        body.append("<h2>SLA Violation Detected</h2>\n");
        body.append(String.format("<p><strong>Metric:</strong> %s</p>\n", metricType));
        body.append(String.format("<p><strong>Compliance Rate:</strong> %.2f%%</p>\n", complianceRate));
        body.append(String.format("<p><strong>Violations:</strong> %d</p>\n", violationCount));
        body.append("<p>Please review the Compliance Dashboard immediately.</p>\n");
        body.append("</body></html>\n");

        sendToAdministrators(subject, body.toString());
    }

    /**
     * Send data retention execution notification.
     *
     * @param entityType Type of entity archived
     * @param recordsArchived Number of records archived
     * @param status Execution status (COMPLETED, FAILED)
     * @param errorMessage Error message if applicable
     */
    public void sendRetentionExecutionNotification(String entityType, long recordsArchived,
                                                   String status, String errorMessage) {
        String subject = String.format("Data Retention Execution: %s - %s", entityType, status);

        StringBuilder body = new StringBuilder();
        body.append("<html><body>\n");
        body.append("<h2>Data Retention Execution Report</h2>\n");
        body.append(String.format("<p><strong>Entity Type:</strong> %s</p>\n", entityType));
        body.append(String.format("<p><strong>Status:</strong> %s</p>\n", status));
        body.append(String.format("<p><strong>Records Archived:</strong> %d</p>\n", recordsArchived));

        if (errorMessage != null && !errorMessage.isEmpty()) {
            body.append(String.format("<p><strong>Error:</strong> %s</p>\n", errorMessage));
        }

        body.append("</body></html>\n");

        sendToComplianceTeam(subject, body.toString());
    }
}

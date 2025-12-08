package com.uniclubconnect.services.notificationservice.service;

import com.uniclubconnect.services.notificationservice.entity.SentEmail;
import com.uniclubconnect.services.notificationservice.repository.SentEmailRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EmailService {

    @Autowired private JavaMailSender mailSender;
    @Autowired private SpringTemplateEngine templateEngine;
    @Autowired private SentEmailRepository sentEmailRepository;

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables, String type) {
        String status = "SENT";
        String errorMessage = null;
        String htmlBody = "";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            Context context = new Context();
            context.setVariables(variables);
            htmlBody = templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("noreply@uniclubconnect.com");

            mailSender.send(message);
            System.out.println("E-posta başarıyla gönderildi: " + to);

        } catch (Exception e) {
            status = "ERROR";
            errorMessage = e.getMessage();
            System.err.println("E-posta gönderme hatası: " + e.getMessage());
        } finally {
            // Ne olursa olsun veritabanına LOG AT
            saveLog(to, subject, htmlBody, type, status, errorMessage);
        }
    }

    private void saveLog(String to, String subject, String body, String type, String status, String error) {
        try {
            String preview = (body != null && body.length() > 500) ? body.substring(0, 500) + "..." : body;

            SentEmail log = SentEmail.builder()
                    .toEmail(to)
                    .subject(subject)
                    .messageType(type)
                    .contentPreview(preview)
                    .status(status)
                    .errorMessage(error)
                    .build();

            sentEmailRepository.save(log);
        } catch (Exception e) {
            System.err.println("Log kaydetme hatası: " + e.getMessage());
        }
    }
}
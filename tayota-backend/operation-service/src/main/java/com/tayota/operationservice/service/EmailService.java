package com.tayota.operationservice.service;

import com.tayota.commoncore.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // Cung cấp các phương thức tạo message và gửi email
    private final JavaMailSender mailSender;

    // Gửi email text thuần (không hỗ trợ HTML)
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            // Tạo một SimpleMailMessage (message text đơn giản)
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            // Gửi email thông qua JavaMailSender
            mailSender.send(message);
        }
        catch (Exception e) {
            throw new CustomException(500 , "Failed to send email: " + e.getMessage());
        }
    }

    // Gửi email HTML với hỗ trợ thay thế biến từ template
    public boolean sendHtmlEmail(String to, String subject, String htmlBody, Map<String, String> variables) {
        try {
            // Xử lý template - thay thế các biến ${key} bằng giá trị từ map
            String processedBody = processTemplate(htmlBody, variables);

            // Tạo MimeMessage (message phức tạp hỗ trợ HTML)
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Thiết lập thông tin email
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(processedBody, true); // tham số true có nghĩa là nội dung là HTML (không phải text thuần)

            // Gửi email thông qua JavaMailSender
            mailSender.send(mimeMessage);
            return true;
        }
        catch (MessagingException e) {
            return false;
        }
    }

    // Xử lý template bằng cách thay thế các biến
    private String processTemplate(String template, Map<String, String> variables) {
        String result = template;

        if (variables != null) {
            // Duyệt qua từng entry và hay thế thế biến
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }

    @Async
    public void sendEmailAsync(String to, String subject, String body) {
        sendSimpleEmail(to, subject, body);
    }


}





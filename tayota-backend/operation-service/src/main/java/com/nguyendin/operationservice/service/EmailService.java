package com.nguyendin.operationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // Inject JavaMailSender từ Spring (được cấu hình tự động bởi spring-boot-starter-mail)
    // Cung cấp các phương thức tạo message và gửi email
    private final JavaMailSender mailSender;

    /**
     * Gửi email text thuần (không hỗ trợ HTML) sử dụng SimpleMailMessage
     * @param to      địa chỉ email người nhận
     * @param subject tiêu đề email
     * @param body    nội dung email (text thuần)
     * @return true nếu gửi thành công, false nếu gửi lỗi
     */
    public boolean sendSimpleEmail(String to, String subject, String body) {
        try {
            // Tạo một SimpleMailMessage (message text đơn giản)
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            // Gửi email thông qua JavaMailSender
            mailSender.send(message);
            return true;
        }
        catch (Exception e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * Gửi email HTML với hỗ trợ thay thế biến từ template
     * @param to        địa chỉ email người nhận
     * @param subject   tiêu đề email
     * @param htmlBody  nội dung HTML template (chứa biến dạng ${key})
     * @param variables map chứa các cặp key-value để thay thế trong template
     * @return true nếu gửi thành công, false nếu gửi lỗi
     */
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
            // setText(content, true) - tham số true có nghĩa là nội dung là HTML (không phải text thuần)
            helper.setText(processedBody, true);

            // Gửi email
            mailSender.send(mimeMessage);
            return true;
        }
        catch (MessagingException e) {
            // Lỗi MessagingException xảy ra khi có vấn đề trong quá trình tạo hoặc gửi MimeMessage
            log.error("Failed to send HTML email to: {}. Error: {}", to, e.getMessage());
            return false;
        }
    }

    /**
     * Xử lý template bằng cách thay thế các biến placeholder dạng ${key} bằng giá trị tương ứng từ map variables
     * @param template  chuỗi template chứa các placeholder dạng ${key}
     * @param variables map chứa các cặp key-value để thay thế
     * @return chuỗi đã được xử lý với tất cả biến đã được thay thế
     */
    private String processTemplate(String template, Map<String, String> variables) {
        String result = template;

        // Nếu map variables không null, duyệt qua từng entry và thay thế
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                // Thay thế ${key} bằng value
                // Ví dụ: template = "Hello ${name}", variables = {name: "John"}
                // Kết quả: "Hello John"
                result = result.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }
}





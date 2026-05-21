package com.tayota.userservice.controller;


import com.tayota.userservice.dto.Request.SendEmailRequest;
import com.tayota.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    @Autowired
    private final EmailService emailService;

    /**
     * Send a simple email
     */
    // gửi email
    @PostMapping("/send")
    public ResponseEntity<EmailResponse> sendEmail(@RequestBody SendEmailRequest request) {
        try {
            emailService.sendSimpleEmail(
                    request.getTo(),
                    request.getSubject(),
                    request.getBody()
            );

            return ResponseEntity.ok(new EmailResponse(true, "Email sent successfully"));
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmailResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Send an HTML email with variables
     */
    @PostMapping("/send-html")
    public ResponseEntity<EmailResponse> sendHtmlEmail(@RequestBody SendEmailRequest request) {
        try {
            boolean success = emailService.sendHtmlEmail(
                    request.getTo(),
                    request.getSubject(),
                    request.getBody(),
                    request.getVariables()
            );

            if (success) {
                return ResponseEntity.ok(new EmailResponse(true, "HTML email sent successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new EmailResponse(false, "Failed to send HTML email"));
            }
        } catch (Exception e) {
            log.error("Error sending HTML email: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmailResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Response class for email sending
     */
    public static class EmailResponse {
        public boolean success;
        public String message;

        public EmailResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}


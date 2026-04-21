package com.nguyendin.operationservice.grpc;

import com.tayota.operationservice.grpc.notification.NotificationServiceGrpc;
import com.tayota.operationservice.grpc.notification.SendEmailRequest;
import com.tayota.operationservice.grpc.notification.SendEmailResponse;
import com.nguyendin.operationservice.service.EmailService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

// ==== gRPC Server cho Notification Service ====
// Triển khai gRPC server để các service khác có thể gọi gửi email qua gRPC
// Tương tự như REST API nhưng dùng gRPC protocol (nhanh hơn, nhẹ hơn)
// Generated code từ notification.proto file
@Slf4j
@GrpcService
public class NotificationGrpcServer extends NotificationServiceGrpc.NotificationServiceImplBase {

    // Constructor Injection cho EmailService
    // EmailService sẽ được Spring tự động inject khi khởi tạo
    private final EmailService emailService;

    public NotificationGrpcServer(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Xử lý request từ client gọi sendEmail thông qua gRPC
     * @param request        SendEmailRequest chứa thông tin: email, subject, body, variables
     * @param responseObserver StreamObserver để gửi response về client
     */
    @Override
    public void sendEmail(SendEmailRequest request, StreamObserver<SendEmailResponse> responseObserver) {
        try {
            log.info("Received sendEmail request: {}", request);
            // Gọi EmailService để gửi email
            // request.getVariablesMap() là Map<String, String> chứa các biến để thay thế trong template
            boolean success = emailService.sendHtmlEmail(
                    request.getEmail(),
                    request.getSubject(),
                    request.getBody(),
                    request.getVariablesMap()
            );

            // Tạo gRPC response
            // Response chỉ chứa một field: success (boolean)
            SendEmailResponse response = SendEmailResponse.newBuilder()
                    .setSuccess(success)
                    .build();

            // Gửi response về client
            // onNext() gửi response
            // onCompleted() báo hiệu stream kết thúc (bắt buộc trong unary RPC)
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (Exception e) {
            // Nếu có exception, gửi lỗi về client thông qua gRPC
            log.error("Error in sendEmail: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }
}



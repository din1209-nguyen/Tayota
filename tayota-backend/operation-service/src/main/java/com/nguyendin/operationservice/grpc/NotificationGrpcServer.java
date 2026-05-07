package com.nguyendin.operationservice.grpc;

import com.tayota.operationservice.grpc.notification.NotificationServiceGrpc;
import com.tayota.operationservice.grpc.notification.SendEmailRequest;
import com.tayota.operationservice.grpc.notification.SendEmailResponse;
import com.nguyendin.operationservice.service.EmailService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@RequiredArgsConstructor
@GrpcService
public class NotificationGrpcServer extends NotificationServiceGrpc.NotificationServiceImplBase {
    private final EmailService emailService;

    private static final String HEADER = "Chào bạn,\n\n";
    private static final String FOOTER = "\n\nTrân trọng,\nĐội ngũ Tayota.";

    // Xử lý request từ client gọi sendEmail thông qua gRPC
    @Override
    public void sendEmail(SendEmailRequest request, StreamObserver<SendEmailResponse> responseObserver) {
        try {
            // Gọi EmailService để gửi email
            boolean success = emailService.sendSimpleEmail(
                    request.getEmail(),
                    request.getSubject(),
                    HEADER + request.getBody() + FOOTER
            );

            // Tạo Response chỉ chứa một field
            SendEmailResponse response = SendEmailResponse.newBuilder()
                    .setSuccess(success)
                    .build();

            // Gửi response về client
            responseObserver.onNext(response); // gửi response
            responseObserver.onCompleted(); // báo hiệu stream kết thúc
        }
        catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}



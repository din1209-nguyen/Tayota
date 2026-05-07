package com.tayota.userservice.grpc;

import com.tayota.userservice.grpc.notification.SendEmailRequest;
import com.tayota.userservice.grpc.notification.SendEmailResponse;
import com.tayota.userservice.grpc.notification.NotificationServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NotificationGrpcClient {
    // Khai báo Stub bất đồng bộ (Async Stub) thay vì Blocking Stub
    private final NotificationServiceGrpc.NotificationServiceStub asyncStub;
    private final ManagedChannel channel;

    public NotificationGrpcClient(
            @Value("${grpc.notification-service.host:operation-service}") String notificationServiceHost,
            @Value("${grpc.notification-service.port:8193}") int notificationServicePort
    ) {
        // ManagedChannel: lớp trừu tượng đại diện cho một kết nối logic đến Server: duy trì kết nối, tự động kết nối lại khi mất mạng, cân bằng tải và đóng kết nối khi không dùng nữa
        // ManagedChannelBuilder: Vì việc cấu hình một kết nối có rất nhiều thông số (địa chỉ, cổng, bảo mật, nén dữ liệu...), gRPC sử dụng mẫu thiết kế Builder Pattern. Thay vì viết một hàm khởi tạo dài, nên dùng Builder để "chọn" từng tính năng một
        this.channel = ManagedChannelBuilder
                // Cung cấp địa chỉ IP và Port
                .forAddress(notificationServiceHost, notificationServicePort)
                // Vô hiệu hóa mã hóa TLS/SSL (chỉ nên dùng ở môi trường local/dev)
                .usePlaintext()
                // Tạo đối tượng channel
                .build();

        // Khởi tạo Async Stub từ channel dùng chung - Stub này sẽ được tái sử dụng cho tất cả requests
        this.asyncStub = NotificationServiceGrpc.newStub(channel);
    }

    // Cleanup channel khi ứng dụng tắt
    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                channel.shutdownNow();
            }
        }
    }

    // Gửi email bất đồng bộ qua gRPC
    public void sendEmailAsync(String email, String subject, String body) {
        // Khởi tạo đối tượng Request gRPC
        SendEmailRequest request = SendEmailRequest.newBuilder()
                .setEmail(email)
                .setSubject(subject)
                .setBody(body)
                .build();

        // Gửi request bất đồng bộ
        asyncStub
                // Thiết lập timeout sau 5 giây không có phản hồi từ Server, kết nối sẽ tự động ngắt để tránh treo tài nguyên hệ thống
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                // Thực hiện gửi request và đăng ký một 'Listener' (StreamObserver), để lắng nghe các sự kiện trả về từ Server
                .sendEmail(request, new StreamObserver<SendEmailResponse>() {
                    @Override
                    public void onNext(SendEmailResponse r) {}

                    @Override
                    public void onError(Throwable t) {
                        log.error("Lỗi gọi gRPC khi gửi email tới: {}. Error: {}", email, t.getMessage(), t);
                    }

                    @Override
                    public void onCompleted() {}
                });
    }
}
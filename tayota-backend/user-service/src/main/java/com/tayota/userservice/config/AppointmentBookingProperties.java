package com.tayota.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;

// Lớp này dùng để đọc các cấu hình liên quan đến việc đặt lịch hẹn từ application.properties
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "appointment.booking")
public class AppointmentBookingProperties {
    // Múi giờ của doanh nghiệp, ví dụ: "Asia/Ho_Chi_Minh"
    private ZoneId businessZone;

    // Giới hạn số lượng cuộc hẹn mà một người dùng đã đăng ký có thể đặt trong một ngày
    private int userDailyLimit;

    // Giới hạn số lượng cuộc hẹn mà một khách hàng chưa đăng ký có thể đặt trong một ngày
    private int guestDailyLimit;

    // Thời gian chờ giữa các lần đặt lịch hẹn cho người dùng đã đăng ký, ví dụ: 30 phút
    private Duration userCooldown;

        // Thời gian chờ giữa các lần đặt lịch hẹn cho khách hàng chưa đăng ký, ví dụ: 60 phút
    private Duration guestCooldown;

    // Thời gian tối thiểu trước khi cuộc hẹn có thể được đặt, ví dụ: 12 giờ
    private Duration minimumNotice;
}

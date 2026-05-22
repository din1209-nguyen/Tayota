package com.tayota.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "appointment.booking")
public class AppointmentBookingProperties {
    private ZoneId businessZone;
    private Duration slotDuration;
    private int userDailyLimit;
    private int guestDailyLimit;
    private Duration userCooldown;
    private Duration guestCooldown;
    private List<LocalTime> allowedStartTimes;
}
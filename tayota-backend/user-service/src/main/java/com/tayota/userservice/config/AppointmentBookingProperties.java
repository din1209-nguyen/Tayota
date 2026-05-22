package com.tayota.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "appointment.booking")
public class AppointmentBookingProperties {
    private ZoneId businessZone;
    private int userDailyLimit;
    private int guestDailyLimit;
    private Duration userCooldown;
    private Duration guestCooldown;
}

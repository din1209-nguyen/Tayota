package com.tayota.operationservice.service.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SystemCacheServiceTest {

    @Test
    void storesValueUntilTtlExpires() throws InterruptedException {
        SystemCacheService cache = new SystemCacheService();

        cache.put("otp:test", "123456", Duration.ofMillis(80));

        assertThat(cache.get("otp:test")).isEqualTo("123456");
        assertThat(cache.hasKey("otp:test")).isTrue();

        Thread.sleep(120);

        assertThat(cache.get("otp:test")).isNull();
        assertThat(cache.hasKey("otp:test")).isFalse();
    }

    @Test
    void incrementsCounterAndKeepsAssignedTtl() throws InterruptedException {
        SystemCacheService cache = new SystemCacheService();

        assertThat(cache.increment("limit:test")).isEqualTo(1L);
        cache.expire("limit:test", Duration.ofMillis(100));
        assertThat(cache.increment("limit:test")).isEqualTo(2L);

        Thread.sleep(140);

        assertThat(cache.get("limit:test")).isNull();
    }

    @Test
    void storesSetMembersWithTtl() throws InterruptedException {
        SystemCacheService cache = new SystemCacheService();

        cache.setAdd("sessions:user-1", "device-1");
        cache.setAdd("sessions:user-1", "device-2");
        cache.expire("sessions:user-1", Duration.ofMillis(100));
        cache.setRemove("sessions:user-1", "device-1");

        assertThat(cache.setMembers("sessions:user-1")).isEqualTo(Set.of("device-2"));

        Thread.sleep(140);

        assertThat(cache.setMembers("sessions:user-1")).isEmpty();
    }
}

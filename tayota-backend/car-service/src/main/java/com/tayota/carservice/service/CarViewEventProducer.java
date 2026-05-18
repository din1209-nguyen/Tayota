package com.tayota.carservice.service;

import com.tayota.carservice.object.CarVersionViewEvent;
import com.tayota.carservice.object.UserBehaviorLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarViewEventProducer {
    private static final String USER_VIEW_HISTORY_TOPIC = "USER_VIEW_HISTORY";
    private static final String USER_BEHAVIOR_LOG_TOPIC = "USER_BEHAVIOR_LOG";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendViewHistory(CarVersionViewEvent event) {
        send(USER_VIEW_HISTORY_TOPIC, event.getCarVersionId().toString(), event);
    }

    public void sendBehaviorLog(UserBehaviorLogEvent event) {
        send(USER_BEHAVIOR_LOG_TOPIC, event.getActionType(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn("Cannot publish Kafka event to topic {}: {}", topic, exception.getMessage());
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn("Cannot serialize Kafka event for topic {}: {}", topic, exception.getMessage());
        }
    }
}

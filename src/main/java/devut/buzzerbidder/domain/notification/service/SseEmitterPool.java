package devut.buzzerbidder.domain.notification.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterPool {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;
    private final Map<String, Set<SseEmitter>> channelEmitters = new ConcurrentHashMap<>();

    private static final long HEARTBEAT_INTERVAL = 30_000L;

    // 개인 채널 : "user:{userId}"
    // 경매 채널 : "auction:{auctionId}"

    /**
     * 특정 채널 구독
     * 예: subscribe("user:123", emitter) - 개인 알림
     * 예: subscribe("auction:456, emitter) - 경매 456 그룹 알림
     */
    public SseEmitter subscribe(String channel) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        channelEmitters.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet())
            .add(emitter);

        emitter.onCompletion(() -> removeEmitter(channel, emitter));
        emitter.onTimeout(() -> removeEmitter(channel, emitter));
        emitter.onError((e) -> removeEmitter(channel, emitter));

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            removeEmitter(channel, emitter);
        }

        return emitter;
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    public void heartbeat() {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        channelEmitters.values().forEach(emitters ->
            emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            })
        );

        deadEmitters.forEach(emitter -> {
            emitter.complete();
            removeEmitterByEmitter(emitter);
        });
    }

    private void removeEmitterByEmitter(SseEmitter emitter) {
        channelEmitters.forEach((channel, emitters) -> {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                channelEmitters.remove(channel);
            }
        });
    }

    /**
     * 특정 채널의 모든 구독자에게 전송
     */
    public void sendToChannel(String channel, Object data) {
        Set<SseEmitter> emitters = channelEmitters.get(channel);
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name("notification")
                    .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        deadEmitters.forEach(emitter -> removeEmitter(channel, emitter));
    }

    /**
     * 여러 채널에 동시 전송 (예 : 개인 + 경매 채널)
     */
    public void sendToChannels(List<String> channels, Object data) {
        channels.forEach(channel -> sendToChannel(channel, data));
    }

    private void removeEmitter(String channel, SseEmitter emitter) {
        Set<SseEmitter> emitters = channelEmitters.get(channel);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                channelEmitters.remove(channel);
            }
        }
    }
}

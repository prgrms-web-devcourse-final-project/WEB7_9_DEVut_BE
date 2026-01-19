package devut.buzzerbidder.domain.chat.service;

import devut.buzzerbidder.domain.chat.dto.ParticipantCountMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomParticipantService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String PARTICIPANT_COUNT_KEY_PREFIX = "auction:participants:";

    private static final String SESSION_MAPPING_KEY = "websocket:session";

    private static final String PARTICIPANT_DESTINATION_PREFIX = "/receive/chat/auction/";

    /**
     * 사용자가 경매 채팅방에 입장할 때 호출
     */
    public void addParticipant(Long auctionId, Long userId) {
        String key = PARTICIPANT_COUNT_KEY_PREFIX + auctionId;

        // Redis Set에 userId 추가
        stringRedisTemplate.opsForSet().add(key, userId.toString());

        // 현재 Set의 크기(참여자 수) 조회
        Long count = stringRedisTemplate.opsForSet().size(key);

        log.debug("{} 번 경매방 참여: 사용자 {}, 현재 참여자 수 = {}", auctionId, userId, count);
        broadcastParticipantCount(auctionId, count);
    }

    /**
     * 사용자가 경매 채팅방에서 퇴장할 때 호출
     */
    public void removeParticipant(Long auctionId, Long userId) {
        String key = PARTICIPANT_COUNT_KEY_PREFIX + auctionId;

        // Redis Set에서 userId 제거
        stringRedisTemplate.opsForSet().remove(key, userId.toString());

        // 현재 Set의 크기 조회
        Long count = stringRedisTemplate.opsForSet().size(key);

        log.debug("{} 번 경매방 퇴장: 사용자 {}, 현재 참여자 수 = {}", auctionId, userId, count);
        broadcastParticipantCount(auctionId, count);
    }

    /**
     * 현재 참여자 수 조회
     */
    public Long getParticipantCount(Long auctionId) {
        String key = PARTICIPANT_COUNT_KEY_PREFIX + auctionId;
        Long count = stringRedisTemplate.opsForSet().size(key);
        return count != null ? count : 0L;
    }

    /**
     * 경매 종료 시 참여자 수 초기화
     */
    public void resetParticipantCount(Long auctionId) {
        String key = PARTICIPANT_COUNT_KEY_PREFIX + auctionId;
        stringRedisTemplate.delete(key);
        log.debug("{} 번 경매방 참여자 수 초기화", auctionId);
    }

    /**
     * [웹소켓 관리] 세션 정보 저장
     * 웹소켓 연결(Subscribe) 시 SessionID와 경매방/유저 정보를 매핑하여 저장
     */
    public void saveSessionInfo(String sessionId, Long auctionId, Long userId) {
        // Key: websocket:session:{sessionId}
        // Value: {auctionId}:{userId}
        String value = auctionId.toString() + ":" + userId;
        stringRedisTemplate.opsForSet().add(SESSION_MAPPING_KEY, sessionId, value);
    }

    /**
     * [웹소켓 관리] 연결 종료 시 자동 퇴장 처리
     * 저장된 세션 정보를 확인하여 해당 유저를 참여자 목록에서 제거
     */
    public void handleDisconnect(String sessionId) {
        String key =  SESSION_MAPPING_KEY + sessionId;
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value != null) {
            try {
                String[] parts = value.split(":");
                if (parts.length == 2) {
                    long auctionId = Long.parseLong(parts[0]);
                    long userId = Long.parseLong(parts[1]);

                    removeParticipant(auctionId, userId);
                }
            } catch (Exception ignored) {
            } finally {
                stringRedisTemplate.delete(key);
            }
        }
    }

    /**
     * 웹소켓을 통해 참여자 수를 브로드캐스트
     */
    private void broadcastParticipantCount(Long auctionId, Long count) {
        String destination = PARTICIPANT_DESTINATION_PREFIX + auctionId + "/participants";
        ParticipantCountMessageDto message = new ParticipantCountMessageDto("PARTICIPANT_COUNT", count != null ? count : 0L);
        messagingTemplate.convertAndSend(destination, message);
    }

}


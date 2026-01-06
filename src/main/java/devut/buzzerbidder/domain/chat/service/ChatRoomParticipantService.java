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
     * 웹소켓을 통해 참여자 수를 브로드캐스트
     */
    private void broadcastParticipantCount(Long auctionId, Long count) {
        String destination = PARTICIPANT_DESTINATION_PREFIX + auctionId + "/participants";
        ParticipantCountMessageDto message = new ParticipantCountMessageDto("PARTICIPANT_COUNT", count != null ? count : 0L);
        messagingTemplate.convertAndSend(destination, message);
    }

}


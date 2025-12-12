package devut.buzzerbidder.global.config.webSocket;

import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.security.CustomUserDetails;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    @Override
    public Message<?> preSend(@NonNull Message<?> message,@NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // WebSocket 연결 요청(CONNECT) 일 때만 검증 수행
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 헤더에서 토큰 추출
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
            String jwt = null;

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
            } else {
                throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
            }

            // 토큰 검증 및 Payload 추출
            Map<String, Object> payload = authTokenService.payloadOrNull(jwt);

            if (payload == null) {
                log.warn("STOMP CONNECT: JWT payload is null for token: {}", jwt);
                throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
            }

            // Payload에서 사용자 ID 추출
            Long userId = (Long) payload.get("id");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            CustomUserDetails userDetails = new CustomUserDetails(user);

            // 인증 객체 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    userDetails.getPassword(),
                    userDetails.getAuthorities()
            );

            // Accessor에 인증 객체 저장 (세션 Principal로 사용)
            accessor.setUser(authentication);
            log.info("STOMP CONNECT: User {} authenticated and set in accessor.", userId);
        }

        return message;
    }
}
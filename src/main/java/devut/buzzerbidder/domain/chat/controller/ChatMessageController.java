package devut.buzzerbidder.domain.chat.controller;

import devut.buzzerbidder.domain.chat.dto.request.ChatMessageRequest;
import devut.buzzerbidder.domain.chat.service.ChatMessageService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Tag(name = "ChatMessage", description = "채팅 메세지 api")
@Controller
@RequiredArgsConstructor
@MessageMapping("/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    // send/chat/auction/{auctionId}
    @Tag(name = "ChatMessage", description = "채팅 메세지 api")
    @MessageMapping("/auction/{auctionId}")
    public void sendAuctionMessage(
            @DestinationVariable Long auctionId,
            @Payload ChatMessageRequest request,
            Principal principal
    ) {
        // Principal 타입이 아닐 경우 예외처리
        if (!(principal instanceof Authentication authentication)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        User sender = userDetails.getUser();

        if (sender == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }


        chatMessageService.sendAuctionMessage(auctionId, sender, request);
    }

    // send/chat/dm/{roomId}
    @MessageMapping("/dm/{roomId}")
    public void sendDirectMessage(
            @DestinationVariable Long roomId,
            @Payload ChatMessageRequest request, // (DTO 필요)
            Principal principal
    ) {
        // Principal 타입이 아닐 경우 예외처리
        if (!(principal instanceof Authentication authentication)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        User sender = userDetails.getUser();

        if (sender == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        chatMessageService.sendDirectMessage(roomId, sender, request);
    }
}

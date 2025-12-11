package devut.buzzerbidder.domain.chat.controller;

import devut.buzzerbidder.domain.chat.dto.request.AuctionChatMessageRequest;
import devut.buzzerbidder.domain.chat.service.ChatMessageService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@MessageMapping("/api/v1/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final RequestContext requestContext;

    @MessageMapping("/auction/{chatRoomId}")
    public void sendAuctionMessage(
            @DestinationVariable Long chatRoomId,
            @Payload AuctionChatMessageRequest request
    ) {

        User user = requestContext.getCurrentUser();

        chatMessageService.sendAuctionMessage(chatRoomId, user, request);
    }
}

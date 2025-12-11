package devut.buzzerbidder.domain.chat.controller;

import devut.buzzerbidder.domain.chat.dto.response.AuctionChatEnterResponse;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.service.ChatRoomService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatrooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final RequestContext requestContext;

    @PutMapping("/{auctionId}/enter")
    public ApiResponse<AuctionChatEnterResponse> enterAuctionChat(
            @PathVariable Long auctionId) {

        // 사용자 정보 조회
        User user = requestContext.getCurrentUser();

        // 채팅바 조회/생성
        ChatRoom chatRoom = chatRoomService.getOrCreateAuctionChatRoom(auctionId);

        // 사용자 참여 상태 갱신
        chatRoomService.enterChatRoom(user, chatRoom);

        AuctionChatEnterResponse response = new AuctionChatEnterResponse(chatRoom.getId());

        return ApiResponse.ok("경매방 채팅 입장 처리 완료", response);
    }
}

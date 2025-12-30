package devut.buzzerbidder.domain.chat.controller;

import devut.buzzerbidder.domain.chat.dto.response.AuctionChatEnterResponse;
import devut.buzzerbidder.domain.chat.dto.response.ChatListResponse;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.service.ChatRoomService;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatrooms")
@Tag(name = "ChatRoom", description = "채팅방 API")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final RequestContext requestContext;

    @GetMapping
    @Operation(summary = "DM 목록 조회", description = "특정 사용자의 DM 채팅방 목록을 조회합니다.")
    public ApiResponse<ChatListResponse> getChatList() {
        User user = requestContext.getCurrentUser();

        ChatListResponse response = chatRoomService.getChatList(user);

        return ApiResponse.ok("DM 목록 조회 성공",response);
    }

    @PutMapping("/{auctionId}/enter")
    @Operation(summary = "경매방 채팅 입장", description = "특정 경매방의 채팅방에 입장합니다.")
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

    @DeleteMapping("/auction/{auctionId}/exit")
    @Operation(summary = "경매방 채팅 퇴장", description = "특정 경매방의 채팅방에서 퇴장합니다.")
    public ApiResponse<Void> exitAuctionChat(
            @PathVariable Long auctionId
    ) {

        User user = requestContext.getCurrentUser();

        chatRoomService.exitAuctionChatRoom(auctionId, user);

        return ApiResponse.ok("경매방 채팅 퇴장 처리 완료");
    }

}

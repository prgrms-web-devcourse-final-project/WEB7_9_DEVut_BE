package devut.buzzerbidder.domain.chat.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.domain.chat.dto.response.ChatListResponse;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.entity.ChatRoomEntered;
import devut.buzzerbidder.domain.chat.repository.ChatRoomRepository;
import devut.buzzerbidder.domain.chat.repository.ChatRoomEnteredRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.domain.wallet.service.WalletRedisService;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final WalletRedisService walletRedisService;
    private final WalletService walletService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomEnteredRepository chatRoomEnteredRepository;
    private final AuctionRoomRepository auctionRoomRepository;
    private final LiveItemRepository liveItemRepository;
    private final DelayedItemRepository delayedItemRepository;
    private final UserService userService;

    // 특정 경매 ID에 해당하는 채팅방을 조회하거나, 존재하지 않으면 생성
    public ChatRoom getOrCreateAuctionChatRoom(Long auctionId) {

        Optional<ChatRoom> existingChatRoom = chatRoomRepository.findByAuctionId(auctionId);

        if (existingChatRoom.isPresent()) {
            return existingChatRoom.get();
        }

        ChatRoom newRoom = ChatRoom.builder()
                .roomType(ChatRoom.RoomType.GROUP)
                .referenceType(ChatRoom.ReferenceEntityType.AUCTION_ROOM)
                .referenceEntityId(auctionId)
                .isActive(true)
                .build();

        return chatRoomRepository.save(newRoom);

    }

    // 지연경매용 1:1 채팅방 생성
    public ChatRoom getOrCreateDMChatRoom(Long itemId, User buyer) {

        Optional<ChatRoom> existingRoom = chatRoomRepository.findDmRoomByItemAndUser(itemId, buyer.getId());

        if(existingRoom.isPresent()) {
            return existingRoom.get();
        }

        DelayedItem item = delayedItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        Long sellerId = item.getSellerUserId();

        // 판매자가 자신의 물건에 채팅 시도 시 예외처리
        if (buyer.getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.SELF_CHAT_NOT_ALLOWED);
        }

        ChatRoom newRoom = ChatRoom.builder()
                .roomType(ChatRoom.RoomType.DM)
                .referenceType(ChatRoom.ReferenceEntityType.ITEM)
                .referenceEntityId(itemId)
                .isActive(true)
                .build();
        chatRoomRepository.save(newRoom);

        // 구매자와 판매자 채팅방 입장 처리
        User seller = userService.findById(sellerId);
        ChatRoomEntered buyerEntry = new ChatRoomEntered(buyer, newRoom);
        ChatRoomEntered sellerEntry = new ChatRoomEntered(seller, newRoom);

        return newRoom;

    }

    // 채팅방 참여 상태 관리
    public void enterChatRoom(User user, ChatRoom chatRoom) {

        // 입장하는 채팅방의 종류가 경매방일 시 별도의 프로세스 진행
        if (chatRoom.getReferenceType() == ChatRoom.ReferenceEntityType.AUCTION_ROOM) {
            enterLiveAuctionProcess(user, chatRoom);
        }

        chatRoomEnteredRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseGet(() -> {
                    ChatRoomEntered newEntry = new ChatRoomEntered(user, chatRoom);
                    return chatRoomEnteredRepository.save(newEntry);
                });

    }

    public void exitAuctionChatRoom(Long auctionId, User user) {

        ChatRoom chatRoom = chatRoomRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

        ChatRoomEntered entered = chatRoomEnteredRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT));

        chatRoomEnteredRepository.delete(entered);

    }

    public void enterLiveAuctionProcess (User user, ChatRoom chatRoom) {
        Long userId =  user.getId();
        Long chatRoomId = chatRoom.getReferenceEntityId();
        Long userBizzFromDb = walletService.getBizzBalance(user);

        // 경매방 입장 가능 여부 검증 (LIVE 상태일 때만 입장 가능)
        validateAuctionRoomEntry(chatRoomId);

        // Redis에서 세션 획득하고 보유 bizz 등록
        boolean acquired = walletRedisService.tryAcquireSessionAndInitBalance(userId, chatRoomId, userBizzFromDb, null);
        // 이미 세션이 있으면 ttl만 연장
        if (!acquired) {
            walletRedisService.extendTtl(userId);
        }
    }

    public void validateAuctionRoomEntry(Long auctionId) {
        AuctionRoom auctionRoom = auctionRoomRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_ROOM_NOT_FOUND));

        // LIVE 상태일 때만 입장 가능
        if (auctionRoom.getAuctionStatus() != AuctionRoom.AuctionStatus.LIVE) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_LIVE);
        }
    }

    public ApiResponse<ChatListResponse> getChatList() {

    }
}


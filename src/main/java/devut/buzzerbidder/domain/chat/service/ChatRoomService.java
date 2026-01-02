package devut.buzzerbidder.domain.chat.service;

import devut.buzzerbidder.domain.auctionroom.entity.AuctionRoom;
import devut.buzzerbidder.domain.auctionroom.repository.AuctionRoomRepository;
import devut.buzzerbidder.domain.chat.dto.response.ChatListResponse;
import devut.buzzerbidder.domain.chat.dto.response.ChatRoomDetailResponse;
import devut.buzzerbidder.domain.chat.dto.response.DirectMessageResponse;
import devut.buzzerbidder.domain.chat.entity.ChatMessage;
import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import devut.buzzerbidder.domain.chat.entity.ChatRoomEntered;
import devut.buzzerbidder.domain.chat.repository.ChatMessageRepository;
import devut.buzzerbidder.domain.chat.repository.ChatRoomEnteredRepository;
import devut.buzzerbidder.domain.chat.repository.ChatRoomRepository;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.domain.wallet.service.WalletRedisService;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final WalletRedisService walletRedisService;
    private final WalletService walletService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomEnteredRepository chatRoomEnteredRepository;
    private final AuctionRoomRepository auctionRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
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

        chatRoomEnteredRepository.save(buyerEntry);
        chatRoomEnteredRepository.save(sellerEntry);

        return newRoom;

    }

    // 채팅방 참여 상태 관리
    @Timed(
            value = "buzzerbidder.chat.enter",
            extraTags = {"op", "live_room_enter"},
            histogram = true
    )
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

    @Transactional(readOnly = true)
    public ChatListResponse getChatList(User me) {
        // 내 입장 정보 및 방 정보 한꺼번에 조회 (N+1 방지)
        List<ChatRoomEntered> myEntries = chatRoomEnteredRepository.findAllMyDmEntries(me);

        if (myEntries.isEmpty()) {
            return new ChatListResponse(Collections.emptyList());
        }

        // 상대방 유저 정보를 가져오기 위해 방 목록 추출
        List<ChatRoom> myRooms = myEntries.stream()
                .map(ChatRoomEntered::getChatRoom)
                .toList();

        // 상대 유저들의 정보 조회
        List<ChatRoomEntered> counterpartEntries = chatRoomEnteredRepository.findCounterparts(myRooms, me);

        // 방 ID별로 상대방 정보를 매핑
        Map<Long, User> counterpartMap = counterpartEntries.stream()
                .collect(Collectors.toMap(e -> e.getChatRoom().getId(), ChatRoomEntered::getUser));

        // DTO 변환 및 안 읽은 메시지 여부 판단
        List<ChatListResponse.ChatRoomItem> items = myEntries.stream()
                .map(entry -> {
                    ChatRoom room = entry.getChatRoom();
                    User otherUser = counterpartMap.get(room.getId());

                    // 마지막 메시지 ID 비교를 통해 안 읽은 상태 확인
                    boolean hasUnread = room.getLastMessageId() != null &&
                            room.getLastMessageId() > entry.getLastReadMessageID();

                    return new ChatListResponse.ChatRoomItem(
                            room.getId(),
                            otherUser.getNickname(),
                            otherUser.getProfileImageUrl(),
                            room.getLastMessageContent(),
                            room.getLastMessageTime(),
                            hasUnread
                    );
                }).toList();

        return new ChatListResponse(items);
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getChatRoomDetail(Long chatRoomId, User user) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

        if (!chatRoomEnteredRepository.existsByUserAndChatRoom(user, chatRoom)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 상품 정보 조회 (ReferenceEntityType이 ITEM인 경우)
        ChatRoomDetailResponse.ItemInfo itemInfo = null;
        if (chatRoom.getReferenceType() == ChatRoom.ReferenceEntityType.ITEM) {
            DelayedItem item = delayedItemRepository.findDelayedItemWithImagesById(chatRoom.getReferenceEntityId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

            String firstImageUrl = item.getImages().isEmpty() ? null : item.getImages().getFirst().getImageUrl();

            itemInfo = new ChatRoomDetailResponse.ItemInfo(
                    item.getId(),
                    item.getName(),
                    item.getCurrentPrice(),
                    firstImageUrl,
                    item.getAuctionStatus().name()
            );
        }

        // 메시지 내역 조회 (ChatMessageRepository에 별도 쿼리 필요)
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomOrderByCreateDateAsc(chatRoom);

        List<DirectMessageResponse> messageResponses = chatMessages.stream()
                .map(m -> new DirectMessageResponse(
                        "CHAT_MESSAGE",
                        m.getId(),
                        m.getSender().getProfileImageUrl(),
                        m.getSender().getNickname(),
                        m.getMessage(),
                        m.getCreateDate()
                )).toList();

        return new ChatRoomDetailResponse(itemInfo, messageResponses);
    }
}


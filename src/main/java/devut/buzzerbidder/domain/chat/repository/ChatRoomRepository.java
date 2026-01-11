package devut.buzzerbidder.domain.chat.repository;

import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 경매방 입장 시 해당 채팅방의 존재 유무 확인에 사용.
     * referenceType이 AUCTION_ROOM인 ChatRoom 객체를 auctionId로 조회.
     * @param auctionId 참조 엔티티 ID
     * @return 해당 조건을 만족하는 ChatRoom 객체를 Optional로 반환.
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.roomType = 'GROUP' " +
            "AND cr.referenceType = 'AUCTION_ROOM' " +
            "AND cr.referenceEntityId = :auctionId")
    Optional<ChatRoom> findByAuctionId(@Param("auctionId") Long auctionId);

    /**
     * 특정 아이템에 대해 특정 유저(구매자)가 참여 중인 1:1 채팅방 조회
     * @param itemId 상품 ID
     * @param userId 유저 ID
     */
    @Query("""
    SELECT cre.chatRoom FROM ChatRoomEntered cre
     WHERE cre.chatRoom.roomType = 'DM'
       AND cre.chatRoom.referenceType = 'ITEM'
       AND cre.chatRoom.referenceEntityId = :itemId
       AND cre.user.id = :userId
    """)
    Optional<ChatRoom> findDmRoomByItemAndUser(@Param("itemId") Long itemId,
                                               @Param("userId") Long userId);

    /**
     * 라이브 경매 아이템에 대해 특정 유저(구매자)가 참여 중인 1:1 채팅방 조회
     * @param liveItemId 라이브 경매 상품 ID
     * @param userId 유저 ID
     */
    @Query("""
    SELECT cre.chatRoom FROM ChatRoomEntered cre
     WHERE cre.chatRoom.roomType = 'DM'
       AND cre.chatRoom.referenceType = 'LIVE_ITEM'
       AND cre.chatRoom.referenceEntityId = :liveItemId
       AND cre.user.id = :userId
    """)
    Optional<ChatRoom> findDmRoomByLiveItemAndUser(@Param("liveItemId") Long liveItemId,
                                                   @Param("userId") Long userId);

    /**
     * 동시성 방지를 위해 현재 저장된 ID보다 더 큰(최신) ID가 들어올 때만 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatRoom cr SET cr.lastMessageId = :id, cr.lastMessageContent = :content, cr.lastMessageTime = :time " +
            "WHERE cr.id = :roomId AND (cr.lastMessageId IS NULL OR cr.lastMessageId < :id)")
    void updateLastMessage(@Param("roomId") Long roomId,
                           @Param("id") Long id,
                           @Param("content") String content,
                           @Param("time") LocalDateTime time);
}

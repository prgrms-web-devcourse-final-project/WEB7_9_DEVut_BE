package devut.buzzerbidder.domain.chat.repository;

import devut.buzzerbidder.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}

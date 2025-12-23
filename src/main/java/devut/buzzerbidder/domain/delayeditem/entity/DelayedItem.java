package devut.buzzerbidder.domain.delayeditem.entity;

import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemCreateRequest;
import devut.buzzerbidder.domain.delayeditem.dto.request.DelayedItemModifyRequest;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DelayedItem extends BaseEntity {

    // 판매자
    @Column(name = "seller_user_id", nullable = false)
    @NotNull(message = "판매자는 필수입니다.")
    @Positive
    private Long sellerUserId;

    // 기본 정보
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @Column(name = "category", length = 20)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "description", columnDefinition = "TEXT")
    @NotBlank(message = "상품 설명은 필수입니다.")
    @Size(max = 2000, message = "상품 설명은 최대 2,000자까지 입력 가능합니다.")
    private String description;

    // 가격 정보
    @Column(name = "start_price", nullable = false)
    @NotNull
    @Min(1)
    private Long startPrice;

    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    @Column(nullable = true)
    private Long buyNowPrice;

    // 시간 정보
    @Column(name = "end_time", nullable = false)
    @NotNull
    private LocalDateTime endTime;

    // 상태
    @Column(name = "item_status", nullable = false, length = 20)
    @NotNull(message = "상품 상태는 필수입니다.")
    @Enumerated(EnumType.STRING)
    private ItemStatus itemStatus;

    @Column(name = "auction_status", nullable = false, length = 20)
    @NotNull(message = "경매 상태는 필수입니다.")
    @Enumerated(EnumType.STRING)
    private AuctionStatus auctionStatus;

    // 배송 정보
    @Column(name = "delivery_include")
    @NotNull(message = "배송비 포함 여부는 필수입니다.")
    private Boolean deliveryInclude;

    @Column(name = "direct_deal_available")
    @NotNull(message = "직거래 가능여부는 필수입니다.")
    private Boolean directDealAvailable;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "preferred_place", length = 100)
    private String preferredPlace;

    // 이미지
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "delayedItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DelayedItemImage> images = new ArrayList<>();

    public DelayedItem(DelayedItemCreateRequest request, User user) {
        this.sellerUserId = user.getId();
        this.name = request.name();
        this.category = request.category();
        this.description = request.description();
        this.startPrice = request.startPrice();
        this.currentPrice = request.startPrice();
        this.buyNowPrice = request.buyNowPrice();
        this.auctionStatus = AuctionStatus.BEFORE_BIDDING;
        this.endTime = request.endTime();
        this.itemStatus = request.itemStatus();
        this.deliveryInclude = request.deliveryInclude();
        this.directDealAvailable = request.directDealAvailable();
        this.region = request.region();
        this.preferredPlace = request.preferredPlace();
        this.images = new ArrayList<>();
    }

    // Enum 정의
    public enum Category {
        CLOTHES, ENTERTAINMENT, ELECTRONICS, COLLECTIBLES, SPORTS,
        SHOES, BAGS, PLATES, ART, MOVIE
    }

    public enum ItemStatus {
        NEW, USED_LIKE_NEW, USED_HEAVILY
    }

    public enum AuctionStatus {
        BEFORE_BIDDING,    // 입찰 전
        IN_PROGRESS,       // 입찰 중
        ENDED,             // 경매 종료 (낙찰)
        FAILED,            // 유찰 (입찰자 없음)
        IN_DEAL,           // 거래 중
        PURCHASE_CONFIRMED // 구매 확정
    }

    // 비즈니스 메서드
    public void updateCurrentPrice(Long newPrice) {
        this.currentPrice = newPrice;
    }

    public void changeAuctionStatus(AuctionStatus newStatus) {
        this.auctionStatus = newStatus;
    }

    public boolean isAuctionEnded() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public boolean canBid() {
        return (auctionStatus == AuctionStatus.BEFORE_BIDDING
            || auctionStatus == AuctionStatus.IN_PROGRESS)
            && !isAuctionEnded();
    }

    public boolean hasBuyNowPrice() {
        return buyNowPrice != null && buyNowPrice > 0;
    }

    public void validateBuyNowPrice() {
        if (hasBuyNowPrice() && buyNowPrice <= startPrice) {
            throw new BusinessException(ErrorCode.INVALID_BUY_NOW_PRICE);
        }
    }

    public void modifyDelayedItem(DelayedItemModifyRequest request) {
        this.name = request.name();
        this.category = request.category();
        this.description = request.description();
        this.startPrice = request.startPrice();
        this.buyNowPrice = request.buyNowPrice();
        this.endTime = request.endTime();
        this.itemStatus = request.itemStatus();
        this.deliveryInclude = request.deliveryInclude();
        this.directDealAvailable = request.directDealAvailable();
        this.region = request.region();
        this.preferredPlace = request.preferredPlace();
    }

    public void addImage(DelayedItemImage image) { images.add(image); }

    public void deleteImageUrls() { this.images.clear(); }
}

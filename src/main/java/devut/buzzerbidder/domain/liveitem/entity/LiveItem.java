package devut.buzzerbidder.domain.liveitem.entity;

import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemCreateRequest;
import devut.buzzerbidder.domain.liveitem.dto.request.LiveItemModifyRequest;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Max;
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

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveItem extends BaseEntity {

    @Column(name = "seller_user_id", nullable = false)
    @NotNull(message = "판매자는 필수입니다.")
    @Positive
    private Long sellerUserId;

    @Column(name = "auction_id", nullable = false)
    @NotNull(message = "경매방은 필수입니다.")
    @Positive
    private Long auctionId;

    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @Column(name = "category", length = 20)
    @Enumerated(EnumType.STRING)
    private Category category;

    //TODO 얘기해서 채워넣기
    public enum Category {
        ELECTRONICS
    }

    @Column(name = "description", columnDefinition = "TEXT")
    @NotBlank(message = "상품 설명은 필수입니다.")
    @Size(max = 2000, message = "상품 설명은 최대 2,000자까지 입력 가능합니다.")
    private String description;

    @NotNull
    @Min(1)
    @Max(1_000_000_000)
    @Column(name = "init_price", nullable = false)
    private Integer initPrice;

    @Column(name = "delivery_include")
    @NotNull(message = "배송비 포함 여부는 필수입니다.")
    private Boolean deliveryInclude;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "상품 상태는 필수입니다.")
    @Enumerated(EnumType.STRING)
    private ItemStatus Itemstatus;

    public enum ItemStatus {
        NEW, USED_LIKE_NEW, USED_HEAVILY
    }

    @Column(name = "auction_status", nullable = false, length = 20)
    @NotNull(message = "경매 상태는 필수입니다.")
    @Enumerated(EnumType.STRING)
    private AuctionStatus auctionStatus;

    // 순서대로 입찰전, 입찰중, 잔금처리대기, 거래중, 구매확정, 유찰
    public enum AuctionStatus {
        BEFORE_BIDDING, IN_PROGRESS, PAYMENT_PENDING,
        IN_DEAL, PURCHASE_CONFIRMED, FAILED
    }

    // 라이브 경매 날짜
    @Column(name = "live_date")
    @NotNull(message = "경매 일자는 필수입니다.")
    private LocalDateTime liveDate;

    // 직거래 가능 여부
    @Column(name = "direct_deal_available")
    @NotNull(message = "직거래 가능여부는 필수입니다.")
    private Boolean directDealAvailable;

    // 내 위치(지역)
    @Column(name = "region", length = 50)
    private String region;

    // 거래 희망 장소
    @Column(name = "preferred_place", length = 100)
    private String preferredPlace;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "liveItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LiveItemImage> images = new ArrayList<>();


    public void addImage(LiveItemImage image) {
        images.add(image);
    }

    public void deleteImageUrls() {
        this.images.clear();
    }


    public LiveItem(LiveItemCreateRequest request, User user){
        this.sellerUserId = user.getId();
        this.auctionId = request.auctionId();
        this.name = request.name();
        this.category = request.category();
        this.description = request.description();
        this.initPrice = request.initPrice();
        this.deliveryInclude = request.deliveryInclude();
        this.Itemstatus = request.Itemstatus();
        this.auctionStatus = request.auctionStatus();
        this.liveDate = request.liveDate();
        this.directDealAvailable = request.directDealAvailable();
        this.region = request.region();
        this.preferredPlace = request.preferredPlace();
        this.images = new ArrayList<>();
    }


    public void modifyLiveItem(LiveItemModifyRequest request){
        this.auctionId = request.auctionId();
        this.name = request.name();
        this.category = request.category();
        this.description = request.description();
        this.initPrice = request.initPrice();
        this.Itemstatus = request.Itemstatus();
        this.auctionStatus = request.auctionStatus();
        this.liveDate = request.liveDate();
        this.directDealAvailable = request.directDealAvailable();
        this.region = request.region();
        this.preferredPlace = request.preferredPlace();
    }

    public void changeAuctionStatus(AuctionStatus auctionStatus) {
        this.auctionStatus = auctionStatus;
    }



}
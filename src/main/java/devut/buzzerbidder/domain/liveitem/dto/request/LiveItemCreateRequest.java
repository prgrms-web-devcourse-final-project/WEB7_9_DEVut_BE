package devut.buzzerbidder.domain.liveitem.dto.request;

import devut.buzzerbidder.domain.liveitem.entity.LiveItem.Category;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem.ItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record LiveItemCreateRequest(

    @NotBlank(message = "이름은 필수입니다.")
    String name,
    
    @NotNull(message = "카테고리는 필수입니다.")
    Category category,
    
    @NotNull(message = "상품 상태는 필수입니다.")
    ItemStatus itemStatus,
    
    @NotBlank(message = "상품 설명은 필수입니다.")
    @Size(max = 2000, message = "상품 설명은 최대 2,000자까지 입력 가능합니다.")
    String description,
    
    @NotNull(message = "시작 가격은 필수입니다.")
    @Positive(message = "시작 가격은 양수여야 합니다.")
    Long initPrice,
    
    @NotNull(message = "배송비 포함 여부는 필수입니다.")
    Boolean deliveryInclude,
    LocalDateTime startAt,
    Boolean directDealAvailable,
    
    String region,
    
    String preferredPlace,
  
    @NotNull(message = "이미지는 필수입니다.")
    List<String> images,
  
    @NotNull(message = "roomIndex 필수입니다.")
    Long roomIndex
    ) {

}

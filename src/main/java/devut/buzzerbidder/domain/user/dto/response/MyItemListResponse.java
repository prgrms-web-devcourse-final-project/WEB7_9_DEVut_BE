package devut.buzzerbidder.domain.user.dto.response;

import java.util.List;

public record MyItemListResponse(
    List<MyItemResponse> items,
    long totalCount
) {
}

package devut.buzzerbidder.domain.liveitem.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record PagingRequest(
    int page,
    int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page - 1, size); // 1-based → 0-based
    }
}
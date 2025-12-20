package devut.buzzerbidder.domain.user.dto;

import java.time.LocalDateTime;

public record ItemIdAndType(
    Long id,
    String type,
    LocalDateTime createdAt
) {
}

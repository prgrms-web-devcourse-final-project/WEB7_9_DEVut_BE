package devut.buzzerbidder.domain.auctionroom.dto.response;

public record LiveItemDto(
    Long id,
    String title,
    Long amount,
    String image,
    Boolean isLiked
) {
}

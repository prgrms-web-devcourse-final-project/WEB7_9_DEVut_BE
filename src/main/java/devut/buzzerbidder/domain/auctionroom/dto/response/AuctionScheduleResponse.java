package devut.buzzerbidder.domain.auctionroom.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AuctionScheduleResponse(
    LocalDate fromDate,
    LocalDate toDate,
    Integer slotMinutes,
    Integer startHour,
    Integer endHour,
    List<AuctionDaysDto> days
) {

}
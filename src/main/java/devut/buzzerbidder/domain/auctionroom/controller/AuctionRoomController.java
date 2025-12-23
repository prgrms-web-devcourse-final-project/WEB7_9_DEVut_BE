package devut.buzzerbidder.domain.auctionroom.controller;

import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomListResponse;
import devut.buzzerbidder.domain.auctionroom.service.AuctionRoomService;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction/auction-rooms")
@Tag(name = "AuctionRoom", description = "경매방 API")
public class AuctionRoomController {

    private final AuctionRoomService auctionRoomService;

    @GetMapping
    @Operation(summary = "경매방 다건 조회")
    public ApiResponse<AuctionRoomListResponse> getAuctionRooms(
        @RequestParam LocalDate date,
        @RequestParam LocalTime time
    ) {
        LocalDateTime targetTime = LocalDateTime.of(date, time);

        AuctionRoomListResponse response =
            auctionRoomService.getAuctionRooms(targetTime);

        return ApiResponse.ok("경매방 다건 조회", response);
    }


}

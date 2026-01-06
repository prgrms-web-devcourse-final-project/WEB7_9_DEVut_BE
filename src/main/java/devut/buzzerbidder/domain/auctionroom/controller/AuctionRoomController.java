package devut.buzzerbidder.domain.auctionroom.controller;

import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomListResponse;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionRoomResponse;
import devut.buzzerbidder.domain.auctionroom.dto.response.AuctionScheduleResponse;
import devut.buzzerbidder.domain.auctionroom.service.AuctionRoomService;
import devut.buzzerbidder.global.response.ApiResponse;
import devut.buzzerbidder.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction/rooms")
@Tag(name = "AuctionRoom", description = "경매방 API")
public class AuctionRoomController {

    private final AuctionRoomService auctionRoomService;

    @GetMapping
    @Operation(summary = "경매방 다건 조회")
    public ApiResponse<AuctionRoomListResponse> getAuctionRooms(
        @RequestParam LocalDate date,
        @RequestParam LocalTime time,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LocalDateTime targetTime = LocalDateTime.of(date, time);
        Long userId = (userDetails != null) ? userDetails.getId() : null;

        AuctionRoomListResponse response =
            auctionRoomService.getAuctionRooms(targetTime,userId);

        return ApiResponse.ok("경매방 다건 조회", response);
    }

    @GetMapping("/schedule")
    @Operation(summary = "경매방 스케줄 조회")
    public ApiResponse<AuctionScheduleResponse> getAuctionSchedule(
        @RequestParam LocalDate fromDate,
        @RequestParam LocalDate toDate
    ) {

        AuctionScheduleResponse response =
            auctionRoomService.getAuctionSchedule(fromDate,toDate);

        return ApiResponse.ok("경매방 스케줄 조회", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "경매방 단건 조회")
    public ApiResponse<AuctionRoomResponse> getAuctionRoom(
        @PathVariable Long id
    ) {

        AuctionRoomResponse response = auctionRoomService.getAuctionRoom(id);

        return ApiResponse.ok("경매방 단건 조회", response);
    }

}

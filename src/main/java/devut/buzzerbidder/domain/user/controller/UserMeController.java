package devut.buzzerbidder.domain.user.controller;

import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.service.LiveDealService;
import devut.buzzerbidder.domain.deliveryTracking.dto.request.DeliveryRequest;
import devut.buzzerbidder.domain.deliveryTracking.dto.response.DeliveryTrackingResponse;
import devut.buzzerbidder.domain.user.dto.request.UserUpdateRequest;
import devut.buzzerbidder.domain.user.dto.response.UserProfileResponse;
import devut.buzzerbidder.domain.user.dto.response.UserUpdateResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "MyPage", description = "마이페이지 api")
public class UserMeController {

    private final RequestContext requestContext;
    private final LiveDealService liveDealService;
    private final UserService userService;

    @PatchMapping("/deals/{type}/{dealId}/delivery")
    @Operation(summary = "배송 정보 입력")
    public ApiResponse<Void> patchDeliveryInfo(
            @PathVariable String type,
            @PathVariable Long dealId,
            @RequestBody @Valid DeliveryRequest request
    ) {
        User currentUser = requestContext.getCurrentUser();

        AuctionType auctionType = AuctionType.fromString(type);
        if(auctionType != AuctionType.LIVE && auctionType != AuctionType.DELAYED) {
            throw new BusinessException(ErrorCode.DEAL_INVALID_TYPE);
        }

        if(auctionType.equals(AuctionType.LIVE))
            liveDealService.patchDeliveryInfo(currentUser, dealId, request.carrierCode(), request.trackingNumber());
//        TODO: else if ~ 지연경매 코드

        return ApiResponse.ok("배송 정보가 입력되었습니다.", null);
    }

    @GetMapping("/deals/{type}/{dealId}/delivery")
    @Operation(summary = "배송 조회")
    public ApiResponse<DeliveryTrackingResponse> getDeliveryTracking(
            @PathVariable String type,
            @PathVariable Long dealId
    ) {
        User currentUser = requestContext.getCurrentUser();

        AuctionType auctionType = AuctionType.fromString(type);

        DeliveryTrackingResponse trackInfo = null;
        if(auctionType.equals(AuctionType.LIVE))
            trackInfo = liveDealService.track(currentUser, dealId);
//        TODO: else if ~ 지연경매 코드

        return trackInfo != null ? ApiResponse.ok("배송조회 성공", trackInfo) : ApiResponse.error(ErrorCode.DEAL_DELIVERY_INFO_NOT_FOUND, null);
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile() {
        User currentUser = requestContext.getCurrentUser();
        UserProfileResponse response = userService.getMyProfile(currentUser);
        return ApiResponse.ok("회원정보 조회 성공", response);
    }

    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 정보를 수정합니다.")
    @PatchMapping
    public ApiResponse<UserUpdateResponse> updateMyProfile(
            @Valid @RequestBody UserUpdateRequest request) {
        User currentUser = requestContext.getCurrentUser();
        UserUpdateResponse response = userService.updateMyProfile(currentUser, request);
        return ApiResponse.ok("회원정보 수정 성공", response);
    }
}

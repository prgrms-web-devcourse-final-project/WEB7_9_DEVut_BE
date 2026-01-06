package devut.buzzerbidder.domain.user.controller;

import devut.buzzerbidder.domain.deal.enums.AuctionType;
import devut.buzzerbidder.domain.deal.service.DelayedDealService;
import devut.buzzerbidder.domain.deal.service.LiveDealService;
import devut.buzzerbidder.domain.deliveryTracking.dto.request.DeliveryRequest;
import devut.buzzerbidder.domain.deliveryTracking.dto.response.DeliveryTrackingResponse;
import devut.buzzerbidder.domain.user.dto.request.DeliveryAddressCreateRequest;
import devut.buzzerbidder.domain.user.dto.request.DeliveryAddressUpdateRequest;
import devut.buzzerbidder.domain.user.dto.request.UserUpdateRequest;
import devut.buzzerbidder.domain.user.dto.response.DeliveryAddressResponse;
import devut.buzzerbidder.domain.user.dto.response.MyItemListResponse;
import devut.buzzerbidder.domain.user.dto.response.UserDealListResponse;
import devut.buzzerbidder.domain.user.dto.response.UserDealResponse;
import devut.buzzerbidder.domain.user.dto.response.UserProfileResponse;
import devut.buzzerbidder.domain.user.dto.response.UserUpdateResponse;
import devut.buzzerbidder.domain.delayeditem.dto.response.DelayedItemListResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.DeliveryAddressService;
import devut.buzzerbidder.domain.user.service.UserDealService;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.domain.delayedbid.service.DelayedBidService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "MyPage", description = "마이페이지 api")
public class UserMeController {

    private final RequestContext requestContext;
    private final LiveDealService liveDealService;
    private final DelayedDealService delayedDealService;
    private final UserService userService;
    private final UserDealService userDealService;
    private final DeliveryAddressService deliveryAddressService;
    private final DelayedBidService delayedBidService;

    @PatchMapping("/deals/{type}/{dealId}/delivery")
    @Operation(summary = "배송 송장 정보 입력")
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
        else if(auctionType.equals(AuctionType.DELAYED))
            delayedDealService.patchDeliveryInfo(currentUser, dealId, request.carrierCode(), request.trackingNumber());

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
        else if(auctionType.equals(AuctionType.DELAYED))
            trackInfo = delayedDealService.track(currentUser, dealId);

        return trackInfo != null ? ApiResponse.ok("배송조회 성공", trackInfo) : ApiResponse.error(ErrorCode.DEAL_DELIVERY_INFO_NOT_FOUND, (DeliveryTrackingResponse) null);
    }

    @PatchMapping("/deals/{type}/{dealId}/address")
    @Operation(summary = "거래 배송지 주소 수정")
    public ApiResponse<Void> updateDealAddress(
            @PathVariable String type,
            @PathVariable Long dealId,
            @RequestBody @Valid DeliveryAddressUpdateRequest request
    ) {
        User currentUser = requestContext.getCurrentUser();

        AuctionType auctionType = AuctionType.fromString(type);
        if(auctionType != AuctionType.LIVE && auctionType != AuctionType.DELAYED) {
            throw new BusinessException(ErrorCode.DEAL_INVALID_TYPE);
        }

        if(auctionType.equals(AuctionType.LIVE))
            liveDealService.updateDeliveryAddress(currentUser, dealId, request.address(), request.addressDetail(), request.postalCode());
        else if(auctionType.equals(AuctionType.DELAYED))
            delayedDealService.updateDeliveryAddress(currentUser, dealId, request.address(), request.addressDetail(), request.postalCode());

        return ApiResponse.ok("배송지 주소가 수정되었습니다.", null);
    }

    @PatchMapping("/deals/{type}/{dealId}/confirm")
    @Operation(summary = "구매 확정")
    public ApiResponse<Void> confirmPurchase(
        @PathVariable String type,
        @PathVariable Long dealId
    ) {
        User currentUser = requestContext.getCurrentUser();

        AuctionType auctionType = AuctionType.fromString(type);
        if (auctionType != AuctionType.LIVE && auctionType != AuctionType.DELAYED) {
            throw new BusinessException(ErrorCode.DEAL_INVALID_TYPE);
        }

        if (auctionType.equals(AuctionType.LIVE)) {
            liveDealService.confirmPurchase(currentUser, dealId);
        } else if (auctionType.equals(AuctionType.DELAYED)) {
            delayedDealService.confirmPurchase(currentUser, dealId);
        }

        return ApiResponse.ok("구매 확정이 완료되었습니다.", null);
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

    @Operation(summary = "내가 등록한 물품 조회", description = "현재 로그인한 사용자가 등록한 물품 목록을 조회합니다.")
    @GetMapping("/items")
    public ApiResponse<MyItemListResponse> getMyItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String type
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        User currentUser = requestContext.getCurrentUser();
        MyItemListResponse response = userService.getMyItems(currentUser, pageable, type);
        return ApiResponse.ok("물품 목록 조회 성공", response);
    }

    @Operation(summary = "내가 찜한 물품 조회", description = "현재 로그인한 사용자가 찜한 물품 목록을 조회합니다.")
    @GetMapping("/likes")
    public ApiResponse<MyItemListResponse> getMyLikedItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String type
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        User currentUser = requestContext.getCurrentUser();
        MyItemListResponse response = userService.getMyLikedItems(currentUser, pageable, type);
        return ApiResponse.ok("물품 목록 조회 성공", response);
    }

    @Operation(summary = "거래 내역 목록 조회", description = "현재 로그인한 사용자의 거래 내역 목록을 조회합니다.")
    @GetMapping("/deals")
    public ApiResponse<UserDealListResponse> getMyDeals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String type
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        User currentUser = requestContext.getCurrentUser();
        UserDealListResponse response = userDealService.getUserDeals(currentUser, pageable, type);
        return ApiResponse.ok("경매 거래 내역 목록 조회 성공", response);
    }

    @Operation(summary = "거래 내역 상세 조회", description = "현재 로그인한 사용자의 거래 내역 상세 정보를 조회합니다.")
    @GetMapping("/deals/{auctionType}/{dealId}")
    public ApiResponse<UserDealResponse> getMyDeal(
            @PathVariable String auctionType,
            @PathVariable Long dealId
    ) {
        User currentUser = requestContext.getCurrentUser();
        AuctionType type = AuctionType.fromString(auctionType);
        UserDealResponse response = userDealService.getUserDeal(currentUser, type, dealId);
        return ApiResponse.ok("경매 거래 내역 조회 성공", response);
    }

    @Operation(summary = "배송지 목록 조회", description = "현재 로그인한 사용자의 배송지 목록을 조회합니다.")
    @GetMapping("/delivery-addresses")
    public ApiResponse<List<DeliveryAddressResponse>> getDeliveryAddresses() {
        User currentUser = requestContext.getCurrentUser();
        List<DeliveryAddressResponse> response = deliveryAddressService.getDeliveryAddresses(currentUser);
        return ApiResponse.ok("배송지 목록 조회 성공", response);
    }

    @Operation(summary = "배송지 추가", description = "새로운 배송지를 추가합니다.")
    @PostMapping("/delivery-addresses")
    public ApiResponse<DeliveryAddressResponse> createDeliveryAddress(
            @Valid @RequestBody DeliveryAddressCreateRequest request
    ) {
        User currentUser = requestContext.getCurrentUser();
        DeliveryAddressResponse response = deliveryAddressService.createDeliveryAddress(currentUser, request);
        return ApiResponse.ok("배송지가 추가되었습니다.", response);
    }

    @Operation(summary = "배송지 수정", description = "기존 배송지 정보를 수정합니다.")
    @PatchMapping("/delivery-addresses/{addressId}")
    public ApiResponse<DeliveryAddressResponse> updateDeliveryAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody DeliveryAddressUpdateRequest request
    ) {
        User currentUser = requestContext.getCurrentUser();
        DeliveryAddressResponse response = deliveryAddressService.updateDeliveryAddress(currentUser, addressId, request);
        return ApiResponse.ok("배송지가 수정되었습니다.", response);
    }

    @Operation(summary = "배송지 삭제", description = "배송지를 삭제합니다.")
    @DeleteMapping("/delivery-addresses/{addressId}")
    public ApiResponse<Void> deleteDeliveryAddress(
            @PathVariable Long addressId
    ) {
        User currentUser = requestContext.getCurrentUser();
        deliveryAddressService.deleteDeliveryAddress(currentUser, addressId);
        return ApiResponse.ok("배송지가 삭제되었습니다.", null);
    }

    @Operation(summary = "기본 배송지 설정", description = "해당 배송지를 기본 배송지로 설정합니다.")
    @PatchMapping("/delivery-addresses/{addressId}/default")
    public ApiResponse<DeliveryAddressResponse> setDefaultAddress(
            @PathVariable Long addressId
    ) {
        User currentUser = requestContext.getCurrentUser();
        DeliveryAddressResponse response = deliveryAddressService.setDefaultAddress(currentUser, addressId);
        return ApiResponse.ok("기본 배송지가 설정되었습니다.", response);
    }

    @Operation(summary = "내가 입찰 중인 물품 목록 조회", description = "현재 진행 중인 경매 중 내가 입찰한 물품 목록을 조회합니다.")
    @GetMapping("/biditems")
    public ApiResponse<DelayedItemListResponse> getMyBiddingItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        User currentUser = requestContext.getCurrentUser();
        DelayedItemListResponse response = delayedBidService.getMyBiddingItems(currentUser, pageable);
        return ApiResponse.ok("내가 입찰 중인 물품 목록 조회", response);
    }
}

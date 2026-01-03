package devut.buzzerbidder.domain.user.service;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.likedelayed.entity.LikeDelayed;
import devut.buzzerbidder.domain.likedelayed.repository.LikeDelayedRepository;
import devut.buzzerbidder.domain.liveBid.service.LiveBidRedisService;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
import devut.buzzerbidder.domain.likelive.entity.LikeLive;
import devut.buzzerbidder.domain.likelive.repository.LikeLiveRepository;
import devut.buzzerbidder.domain.user.dto.request.EmailLoginRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.dto.request.UserUpdateRequest;
import devut.buzzerbidder.domain.user.dto.response.LoginResponse;
import devut.buzzerbidder.domain.user.dto.response.MyItemListResponse;
import devut.buzzerbidder.domain.user.dto.response.MyItemResponse;
import devut.buzzerbidder.domain.user.dto.response.UserProfileResponse;
import devut.buzzerbidder.domain.user.dto.response.UserUpdateResponse;
import devut.buzzerbidder.domain.user.entity.DeliveryAddress;
import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.DeliveryAddressRepository;
import devut.buzzerbidder.domain.user.repository.ProviderRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final WalletService walletService;
    private final LiveItemRepository liveItemRepository;
    private final DelayedItemRepository delayedItemRepository;
    private final LikeLiveRepository likeLiveRepository;
    private final LikeDelayedRepository likeDelayedRepository;
    private final LiveBidRedisService liveBidRedisService;

    @Transactional
    public LoginResponse signUp(EmailSignUpRequest request) {
        // 이메일 인증 완료 여부 확인
        if (!emailVerificationService.isEmailVerified(request.email())) {
            throw new BusinessException(ErrorCode.USER_EMAIL_NOT_VERIFIED);
        }

        // 이메일로 기존 사용자 조회
        Optional<User> existingUser = userRepository.findByEmail(request.email());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // EMAIL Provider가 이미 존재하는 경우 (이미 이메일로 가입한 계정)
            if (providerRepository.existsByUserAndProviderType(user, Provider.ProviderType.EMAIL)) {
                throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
            }
            
            // 소셜 로그인으로 가입한 계정인 경우 EMAIL Provider 추가 및 비밀번호 설정
            // 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(request.password());
            user.updatePassword(encodedPassword);
            
            // EMAIL Provider 생성
            Provider emailProvider = Provider.builder()
                    .providerType(Provider.ProviderType.EMAIL)
                    .providerId(request.email()) // EMAIL의 경우 email을 providerId로 사용
                    .user(user)
                    .build();
            providerRepository.save(emailProvider);
            emailVerificationService.deleteVerifiedEmail(request.email());
            
            return LoginResponse.of(user);
        }

        // 신규 사용자 생성
        // 닉네임 중복 체크
        // if (userRepository.existsByNickname(request.nickname())) {
        // throw new BusinessException(ErrorCode.USER_NICKNAME_DUPLICATE);
        // }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 회원 생성
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname())
                .profileImageUrl(request.image())
                .role(User.UserRole.USER)
                .build();

        user = userRepository.save(user);

        // Provider 생성 (EMAIL)
        Provider provider = Provider.builder()
                .providerType(Provider.ProviderType.EMAIL)
                .providerId(request.email()) // EMAIL의 경우 email을 providerId로 사용
                .user(user)
                .build();
        providerRepository.save(provider);

        walletService.createWallet(user);

        // 회원가입 완료 후 인증 완료 표시 삭제
        emailVerificationService.deleteVerifiedEmail(request.email());

        return LoginResponse.of(user);
    }

    public LoginResponse login(EmailLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_LOGIN_FAILED));

        // EMAIL Provider가 없는 경우 (소셜 로그인으로만 가입한 계정)
        if (!providerRepository.existsByUserAndProviderType(user, Provider.ProviderType.EMAIL)) {
            throw new BusinessException(ErrorCode.USER_SOCIAL_ACCOUNT);
        }

        // 비밀번호가 null인 경우 (이론적으로는 발생하지 않아야 하지만 안전장치)
        if (user.getPassword() == null) {
            throw new BusinessException(ErrorCode.USER_SOCIAL_ACCOUNT);
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }

        return LoginResponse.of(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public UserProfileResponse getMyProfile(User user) {
        Long bizz = walletService.getBizzBalance(user);
        return UserProfileResponse.from(user, bizz);
    }

    @Transactional
    public UserUpdateResponse updateMyProfile(User user, UserUpdateRequest request) {
        // 이메일 변경 시 중복 체크
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
            }
        }

        // 닉네임 변경 시 중복 체크 (null이 아니고 기존 닉네임과 다를 때만)
        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.nickname())) {
                throw new BusinessException(ErrorCode.USER_NICKNAME_DUPLICATE);
            }
        }

        // 프로필 업데이트
        user.updateProfile(
                request.email(),
                request.nickname(),
                request.image()
        );
        User updatedUser = userRepository.save(user);

        Optional<DeliveryAddress> deliveryAddress = deliveryAddressRepository.findByUser(user);
        if(deliveryAddress.isPresent()) {
            deliveryAddress.get().update(
                    request.address(),
                    request.addressDetail(),
                    request.postalCode()
            );
        } else {
            DeliveryAddress newDeliveryAddress = DeliveryAddress.builder()
                    .user(user)
                    .address(request.address())
                    .addressDetail(request.addressDetail())
                    .postalCode(request.postalCode())
                    .build();
            deliveryAddressRepository.save(newDeliveryAddress);
        }

        return UserUpdateResponse.from(updatedUser);
    }

    public MyItemListResponse getMyItems(User user, Pageable pageable, String type) {
        // 전체 개수 계산
        long totalElements = userRepository.countMyItems(user.getId(), type);
        
        // UNION 쿼리로 ID와 타입만 가져오기 (페이징 적용)
        List<Object[]> results = userRepository.findMyItemIdsAndTypes(
            user.getId(),
            type,
            pageable.getPageSize(),
            pageable.getOffset()
        );

        // 공통 로직으로 처리 (내가 작성한 글이므로 찜 여부 확인 필요)
        List<MyItemResponse> items = fetchAndMapItems(results, user, false);

        return new MyItemListResponse(items, totalElements);
    }

    public MyItemListResponse getMyLikedItems(User user, Pageable pageable, String type) {
        // 전체 개수 계산
        long totalElements = userRepository.countMyLikedItems(user.getId(), type);
        
        // UNION 쿼리로 ID와 타입만 가져오기 (페이징 적용)
        List<Object[]> results = userRepository.findMyLikedItemIdsAndTypes(
            user.getId(),
            type,
            pageable.getPageSize(),
            pageable.getOffset()
        );

        // 공통 로직으로 처리 (찜한 목록이므로 항상 wish = true)
        List<MyItemResponse> items = fetchAndMapItems(results, user, true);

        return new MyItemListResponse(items, totalElements);
    }

    /**
     * ID와 타입 리스트를 받아서 엔티티를 조회하고 DTO로 변환하는 공통 메서드
     * N+1 문제를 해결하기 위해 좋아요 개수도 IN 절로 한 번에 조회
     * @param results ID와 타입 리스트
     * @param user 현재 사용자 (찜 여부 확인용)
     * @param isLikedItems 찜한 목록인지 여부 (true면 항상 wish = true)
     */
    private List<MyItemResponse> fetchAndMapItems(List<Object[]> results, User user, boolean isLikedItems) {
        // ID와 타입 분리
        List<Long> liveItemIds = new ArrayList<>();
        List<Long> delayedItemIds = new ArrayList<>();
        
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            String type = (String) row[1];
            if ("LIVE".equals(type)) {
                liveItemIds.add(id);
            } else {
                delayedItemIds.add(id);
            }
        }

        // 엔티티 조회 (빈 리스트 체크 포함)
        Map<Long, LiveItem> liveItemMap = liveItemIds.isEmpty() 
            ? Collections.emptyMap()
            : liveItemRepository.findLiveItemsWithImages(liveItemIds).stream()
                .collect(Collectors.toMap(LiveItem::getId, item -> item));

        Map<Long, DelayedItem> delayedItemMap = delayedItemIds.isEmpty() 
            ? Collections.emptyMap()
            : delayedItemRepository.findDelayedItemsWithImages(delayedItemIds).stream()
                .collect(Collectors.toMap(DelayedItem::getId, item -> item));

        // 찜 여부 Batch 조회 (N+1 해결) - 찜한 목록이 아닌 경우에만 조회
        Set<Long> likedLiveItemIds = new HashSet<>();
        Set<Long> likedDelayedItemIds = new HashSet<>();
        
        if (!isLikedItems && user != null) {
            // LiveItem 찜 여부 조회
            if (!liveItemIds.isEmpty()) {
                List<LiveItem> liveItems = liveItemMap.values().stream().toList();
                for (LiveItem liveItem : liveItems) {
                    Optional<LikeLive> likeLive = likeLiveRepository.findByUserAndLiveItem(user, liveItem);
                    if (likeLive.isPresent()) {
                        likedLiveItemIds.add(liveItem.getId());
                    }
                }
            }
            
            // DelayedItem 찜 여부 조회
            if (!delayedItemIds.isEmpty()) {
                List<DelayedItem> delayedItems = delayedItemMap.values().stream().toList();
                for (DelayedItem delayedItem : delayedItems) {
                    Optional<LikeDelayed> likeDelayed = likeDelayedRepository.findByUserAndDelayedItem(user, delayedItem);
                    if (likeDelayed.isPresent()) {
                        likedDelayedItemIds.add(delayedItem.getId());
                    }
                }
            }
        }

        // 최종 결과 조립 (순서 유지)
        List<MyItemResponse> items = new ArrayList<>();
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            String type = (String) row[1];

            if ("LIVE".equals(type)) {
                LiveItem item = liveItemMap.get(id);
                if (item != null) {
                    // Redis에서 현재 입찰가 가져오기
                    String redisKey = "liveItem:" + item.getId();
                    String maxBidPriceStr = liveBidRedisService.getHashField(redisKey, "maxBidPrice");
                    Long currentPrice = (maxBidPriceStr != null)
                        ? Long.parseLong(maxBidPriceStr)
                        : item.getInitPrice(); // Redis에 없으면 초기 가격 사용
                    
                    // 찜 여부 확인
                    Boolean wish = isLikedItems ? true : likedLiveItemIds.contains(id);
                    items.add(MyItemResponse.fromLiveItem(item, currentPrice, wish));
                }
            } else if ("DELAYED".equals(type)) {
                DelayedItem item = delayedItemMap.get(id);
                if (item != null) {
                    // 찜 여부 확인
                    Boolean wish = isLikedItems ? true : likedDelayedItemIds.contains(id);
                    items.add(MyItemResponse.fromDelayedItem(item, wish));
                }
            }
        }
        
        return items;
    }
}

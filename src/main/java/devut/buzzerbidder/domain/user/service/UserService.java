package devut.buzzerbidder.domain.user.service;

import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
import devut.buzzerbidder.domain.likedelayed.repository.LikeDelayedRepository;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            walletService.createWallet(user);
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
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findByUser(user).orElse(null);
        return UserProfileResponse.from(user, bizz, deliveryAddress);
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

        Optional<DeliveryAddress> deliveryAddressOptional = deliveryAddressRepository.findByUser(user);
        DeliveryAddress deliveryAddress;
        if(deliveryAddressOptional.isPresent()) {
            deliveryAddress = deliveryAddressOptional.get();
            deliveryAddress.update(
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
            deliveryAddress = deliveryAddressRepository.save(newDeliveryAddress);
        }

        return UserUpdateResponse.from(updatedUser, deliveryAddress);
    }


    public MyItemListResponse getMyItems(User user, Pageable pageable) {
        // 전체 개수 계산
        long totalElements = userRepository.countMyItems(user.getId());
        
        // UNION 쿼리로 ID와 타입만 가져오기 (페이징 적용)
        List<Object[]> results = userRepository.findMyItemIdsAndTypes(
            user.getId(),
            pageable.getPageSize(),
            pageable.getOffset()
        );

        // 공통 로직으로 처리
        List<MyItemResponse> items = fetchAndMapItems(results);

        return new MyItemListResponse(items, totalElements);
    }

    public MyItemListResponse getMyLikedItems(User user, Pageable pageable) {
        // 전체 개수 계산
        long totalElements = userRepository.countMyLikedItems(user.getId());
        
        // UNION 쿼리로 ID와 타입만 가져오기 (페이징 적용)
        List<Object[]> results = userRepository.findMyLikedItemIdsAndTypes(
            user.getId(),
            pageable.getPageSize(),
            pageable.getOffset()
        );

        // 공통 로직으로 처리
        List<MyItemResponse> items = fetchAndMapItems(results);

        return new MyItemListResponse(items, totalElements);
    }

    /**
     * ID와 타입 리스트를 받아서 엔티티를 조회하고 DTO로 변환하는 공통 메서드
     * N+1 문제를 해결하기 위해 좋아요 개수도 IN 절로 한 번에 조회
     */
    private List<MyItemResponse> fetchAndMapItems(List<Object[]> results) {
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

        // 좋아요 개수 Batch 조회 (N+1 해결)
        Map<Long, Long> liveLikesMap = liveItemIds.isEmpty() 
            ? Collections.emptyMap()
            : likeLiveRepository.countByLiveItemIdIn(liveItemIds).stream()
                .collect(Collectors.toMap(
                    row -> ((Number) row[0]).longValue(), 
                    row -> ((Number) row[1]).longValue(),
                    (existing, replacement) -> existing
                ));

        Map<Long, Long> delayedLikesMap = delayedItemIds.isEmpty() 
            ? Collections.emptyMap()
            : likeDelayedRepository.countByDelayedItemIdIn(delayedItemIds).stream()
                .collect(Collectors.toMap(
                    row -> ((Number) row[0]).longValue(), 
                    row -> ((Number) row[1]).longValue(),
                    (existing, replacement) -> existing
                ));

        // 최종 결과 조립 (순서 유지)
        List<MyItemResponse> items = new ArrayList<>();
        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            String type = (String) row[1];

            if ("LIVE".equals(type)) {
                LiveItem item = liveItemMap.get(id);
                if (item != null) {
                    Long likes = liveLikesMap.getOrDefault(id, 0L);
                    items.add(MyItemResponse.fromLiveItem(item, likes));
                }
            } else if ("DELAYED".equals(type)) {
                DelayedItem item = delayedItemMap.get(id);
                if (item != null) {
                    Long likes = delayedLikesMap.getOrDefault(id, 0L);
                    items.add(MyItemResponse.fromDelayedItem(item, likes));
                }
            }
        }
        
        return items;
    }
}

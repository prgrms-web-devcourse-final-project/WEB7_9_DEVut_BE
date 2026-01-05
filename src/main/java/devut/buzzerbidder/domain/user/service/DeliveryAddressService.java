package devut.buzzerbidder.domain.user.service;

import devut.buzzerbidder.domain.user.dto.request.DeliveryAddressCreateRequest;
import devut.buzzerbidder.domain.user.dto.request.DeliveryAddressUpdateRequest;
import devut.buzzerbidder.domain.user.dto.response.DeliveryAddressResponse;
import devut.buzzerbidder.domain.user.entity.DeliveryAddress;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.repository.DeliveryAddressRepository;
import devut.buzzerbidder.domain.user.repository.UserRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryAddressService {

    private final DeliveryAddressRepository deliveryAddressRepository;
    private final UserRepository userRepository;

    // 배송지 목록 조회
    public List<DeliveryAddressResponse> getDeliveryAddresses(User user) {
        List<DeliveryAddress> addresses = deliveryAddressRepository.findAllByUser(user);
        return addresses.stream()
                .map(DeliveryAddressResponse::from)
                .collect(Collectors.toList());
    }

    // 배송지 추가
    @Transactional
    public DeliveryAddressResponse createDeliveryAddress(User user, DeliveryAddressCreateRequest request) {
        // 기본 배송지로 설정하는 경우, 기존 기본 배송지 해제
        if (Boolean.TRUE.equals(request.isDefault())) {
            unsetDefaultAddress(user);
        }

        DeliveryAddress newAddress = DeliveryAddress.builder()
                .user(user)
                .address(request.address())
                .addressDetail(request.addressDetail())
                .postalCode(request.postalCode())
                .isDefault(request.isDefault() != null ? request.isDefault() : false)
                .build();

        DeliveryAddress savedAddress = deliveryAddressRepository.save(newAddress);

        // 기본 배송지로 설정한 경우 User의 defaultDeliveryAddressId 업데이트
        if (savedAddress.getIsDefault()) {
            user.setDefaultDeliveryAddressId(savedAddress.getId());
            userRepository.save(user);
        }

        return DeliveryAddressResponse.from(savedAddress);
    }

    // 배송지 수정
    @Transactional
    public DeliveryAddressResponse updateDeliveryAddress(User user, Long addressId, DeliveryAddressUpdateRequest request) {
        DeliveryAddress address = deliveryAddressRepository.findByUserAndId(user, addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        address.update(
                request.address(),
                request.addressDetail(),
                request.postalCode()
        );

        DeliveryAddress savedAddress = deliveryAddressRepository.save(address);
        return DeliveryAddressResponse.from(savedAddress);
    }

    // 배송지 삭제
    @Transactional
    public void deleteDeliveryAddress(User user, Long addressId) {
        DeliveryAddress address = deliveryAddressRepository.findByUserAndId(user, addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 기본 배송지를 삭제하는 경우
        if (address.getIsDefault()) {
            user.setDefaultDeliveryAddressId(null);
            userRepository.save(user);
        }

        deliveryAddressRepository.delete(address);
    }

    // 기본 배송지 설정
    @Transactional
    public DeliveryAddressResponse setDefaultAddress(User user, Long addressId) {
        DeliveryAddress address = deliveryAddressRepository.findByUserAndId(user, addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_DATA));

        // 기존 기본 배송지 해제
        unsetDefaultAddress(user);

        // 새 기본 배송지 설정
        address.setDefault(true);
        DeliveryAddress savedAddress = deliveryAddressRepository.save(address);

        // User의 defaultDeliveryAddressId 업데이트
        user.setDefaultDeliveryAddressId(savedAddress.getId());
        userRepository.save(user);

        return DeliveryAddressResponse.from(savedAddress);
    }

    // 기존 기본 배송지 해제
    private void unsetDefaultAddress(User user) {
        deliveryAddressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(existingDefault -> {
                    existingDefault.setDefault(false);
                    deliveryAddressRepository.save(existingDefault);
                });
    }
}


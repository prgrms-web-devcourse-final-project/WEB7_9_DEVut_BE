package devut.buzzerbidder.domain.user.repository;

import devut.buzzerbidder.domain.user.entity.DeliveryAddress;
import devut.buzzerbidder.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    List<DeliveryAddress> findAllByUser(User user);
    Optional<DeliveryAddress> findByUserAndId(User user, Long id);
    Optional<DeliveryAddress> findByUserAndIsDefaultTrue(User user);
}

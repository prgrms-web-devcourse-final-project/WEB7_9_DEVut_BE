package devut.buzzerbidder.domain.user.repository;

import devut.buzzerbidder.domain.user.entity.Provider;
import devut.buzzerbidder.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {
    Optional<Provider> findByProviderTypeAndProviderId(Provider.ProviderType providerType, String providerId);
    boolean existsByUserAndProviderType(User user, Provider.ProviderType providerType);
}


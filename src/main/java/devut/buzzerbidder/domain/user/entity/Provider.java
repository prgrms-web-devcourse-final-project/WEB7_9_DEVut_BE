package devut.buzzerbidder.domain.user.entity;

import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "providers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider_type", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Provider extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "provider_type")
    private ProviderType providerType;

    @Column(nullable = false, length = 100, name = "provider_id")
    private String providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Provider(ProviderType providerType, String providerId, User user) {
        this.providerType = providerType;
        this.providerId = providerId;
        this.user = user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public enum ProviderType {
        EMAIL("email", "이메일사용자"),
        KAKAO("kakao", "카카오사용자"),
        NAVER("naver", "네이버사용자"),
        GOOGLE("google", "구글사용자");

        private final String providerName;
        private final String defaultNicknamePrefix;

        ProviderType(String providerName, String defaultNicknamePrefix) {
            this.providerName = providerName;
            this.defaultNicknamePrefix = defaultNicknamePrefix;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getDefaultNicknamePrefix() {
            return defaultNicknamePrefix;
        }
    }
}


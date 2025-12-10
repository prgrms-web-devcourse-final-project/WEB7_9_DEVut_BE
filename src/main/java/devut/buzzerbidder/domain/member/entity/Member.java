package devut.buzzerbidder.domain.member.entity;

import devut.buzzerbidder.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = true, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderType providerType;

    @Column(length = 100)
    private String providerId;

    @Builder
    public Member(String email, String password, String name, String nickname,
                  LocalDate birthDate, String profileImageUrl, MemberRole role,
                  ProviderType providerType, String providerId) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.profileImageUrl = profileImageUrl;
        this.role = role != null ? role : MemberRole.USER;
        this.providerType = providerType != null ? providerType : ProviderType.EMAIL;
        this.providerId = providerId;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }


    public enum MemberRole {
        USER, ADMIN
    }

    public enum ProviderType {
        EMAIL, KAKAO, NAVER, GOOGLE
    }
}


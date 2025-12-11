package devut.buzzerbidder.global.security;

import devut.buzzerbidder.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getNickname() {
        return user.getNickname();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return !user.getDeleted();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.getDeleted();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !user.getDeleted();
    }

    @Override
    public boolean isEnabled() {
        return !user.getDeleted();
    }
}
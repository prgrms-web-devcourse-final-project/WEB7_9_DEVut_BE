package devut.buzzerbidder.global.security;

import devut.buzzerbidder.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {
    
    private final OAuth2User oAuth2User;
    private final User user;
    
    public CustomOAuth2User(OAuth2User oAuth2User, User user) {
        this.oAuth2User = oAuth2User;
        this.user = user;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        // UnmodifiableMap을 수정할 수 없으므로 새로운 HashMap 생성
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("user", user);
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oAuth2User.getAuthorities();
    }
    
    @Override
    public String getName() {
        return oAuth2User.getName();
    }
    
    public User getUser() {
        return user;
    }
}


package devut.buzzerbidder.domain.deliveryTracking.infrastructure;

import org.springframework.stereotype.Component;

@Component
public class DeliveryTrackingTokenStore {

    private String token;
    private long expireAt; // epoch milliseconds

    public synchronized void updateToken(String token, long expiresIn) {
        this.token = token;
        this.expireAt = System.currentTimeMillis() + (expiresIn - 30) * 1000; // 30초 일찍 만료 처리
    }

    public synchronized String getToken() {
        return token;
    }

    public synchronized boolean isExpired() {
        return token == null || System.currentTimeMillis() > expireAt;
    }
}


package devut.buzzerbidder.domain.user.service;

import devut.buzzerbidder.domain.user.dto.response.UserInfoResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final WalletService walletService;

    public UserInfoResponse getUserInfo(User user) {
        Long bizz = walletService.getBizzBalance(user);

        return UserInfoResponse.of(user, bizz);
    }

}

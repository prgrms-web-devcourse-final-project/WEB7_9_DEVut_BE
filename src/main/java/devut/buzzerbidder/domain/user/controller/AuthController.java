package devut.buzzerbidder.domain.user.controller;

import devut.buzzerbidder.domain.user.dto.request.EmailLoginRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailVerificationCodeRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailVerificationRequest;
import devut.buzzerbidder.domain.user.dto.response.EmailVerificationResponse;
import devut.buzzerbidder.domain.user.dto.response.LoginResponse;
import devut.buzzerbidder.domain.user.dto.response.UserInfo;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.domain.user.service.EmailService;
import devut.buzzerbidder.domain.user.service.EmailVerificationService;
import devut.buzzerbidder.domain.user.service.RefreshTokenService;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RequestContext requestContext;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "이메일 인증 코드 발송", description = "회원가입을 위한 이메일 인증 코드를 발송합니다.")
    @PostMapping("/email/verification")
    public ApiResponse<EmailVerificationResponse> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequest request) {
        String code = emailVerificationService.generateAndSaveVerificationCode(request.email());
        emailService.sendVerificationCode(request.email(), code);
        
        Long remainingSeconds = emailVerificationService.getRemainingSeconds(request.email());
        LocalDateTime expiresAt = emailVerificationService.getExpiresAt(request.email());
        EmailVerificationResponse response = new EmailVerificationResponse(remainingSeconds, expiresAt);
        
        return ApiResponse.ok("이메일 인증 코드가 발송되었습니다.", response);
    }

    @Operation(summary = "이메일 인증 코드 검증", description = "발송된 이메일 인증 코드를 검증합니다.")
    @PostMapping("/email/verification/verify")
    public ApiResponse<Void> verifyCode(
            @Valid @RequestBody EmailVerificationCodeRequest request) {
        boolean isValid = emailVerificationService.verifyCode(request.email(), request.code());
        if (!isValid) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return ApiResponse.ok("이메일 인증이 완료되었습니다.");
    }

    @Operation(summary = "회원가입", description = "이메일을 사용한 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public ApiResponse<Void> signUp(
            @Valid @RequestBody EmailSignUpRequest request) {
        userService.signUp(request);
        return ApiResponse.ok("회원가입에 성공했습니다.");
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호를 사용한 로그인을 진행합니다. 성공 시 JWT 토큰이 헤더와 쿠키에 설정됩니다.")
    @PostMapping("/signin")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody EmailLoginRequest request) {
        LoginResponse response = userService.login(request);
        
        // 토큰 생성 및 헤더/쿠키에 설정
        User user = userService.findById(response.userInfo().id());
        String accessToken = authTokenService.genAccessToken(user);
        String refreshToken = authTokenService.genRefreshToken(user);
        setTokensInResponse(response.userInfo().id());
        
        // 응답에 토큰 포함
        LoginResponse responseWithTokens = new LoginResponse(
                response.userInfo(),
                accessToken,
                refreshToken
        );
        
        return ApiResponse.ok("로그인에 성공했습니다.", responseWithTokens);
    }


    @Operation(summary = "AccessToken 재발급", description = "Refresh Token을 사용하여 Access Token을 재발급합니다.")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh() {
        String refreshToken = requestContext.getCookieValue("refreshToken", "");

        // AuthTokenService에서 검증 및 User 조회
        User user = authTokenService.validateAndGetUserByRefreshToken(refreshToken);

        // 새로운 토큰 생성 및 설정
        String newAccessToken = authTokenService.genAccessToken(user);
        String newRefreshToken = authTokenService.genRefreshToken(user);
        setTokensInResponse(user.getId());

        // 응답에 토큰 포함
        LoginResponse response = new LoginResponse(
                UserInfo.from(user),
                newAccessToken,
                newRefreshToken
        );

        return ApiResponse.ok("AccessToken 재발급에 성공했습니다.", response);
    }

    @Operation(summary = "로그아웃", description = "로그아웃을 진행합니다. Refresh Token이 Redis에서 삭제되고 쿠키가 제거됩니다.")
    @PostMapping("/signout")
    public ApiResponse<Void> signOut() {
        // 현재 인증된 사용자 정보 가져오기
        User user = requestContext.getCurrentUser();

        // Redis에서 refresh token 삭제
        refreshTokenService.deleteRefreshToken(user.getId());

        // 쿠키 삭제
        requestContext.deleteCookie("accessToken");
        requestContext.deleteCookie("refreshToken");

        return ApiResponse.ok("로그아웃에 성공했습니다.");
    }
    
    private void setTokensInResponse(Long userId) {
        // User 조회
        User user = userService.findById(userId);
        
        // 토큰 생성
        String accessToken = authTokenService.genAccessToken(user);
        String refreshToken = authTokenService.genRefreshToken(user);
        
        // 헤더에 토큰 설정
        requestContext.setHeader("Authorization", "Bearer " + accessToken);
        requestContext.setHeader("Refresh-Token", refreshToken);
        
        // 쿠키에 토큰 설정
        requestContext.setCookie("accessToken", accessToken);
        requestContext.setCookie("refreshToken", refreshToken);
    }

}

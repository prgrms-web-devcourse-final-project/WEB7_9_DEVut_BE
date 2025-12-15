package devut.buzzerbidder.domain.user.controller;

import devut.buzzerbidder.domain.user.dto.request.EmailLoginRequest;
import devut.buzzerbidder.domain.user.dto.request.EmailSignUpRequest;
import devut.buzzerbidder.domain.user.dto.request.UserUpdateRequest;
import devut.buzzerbidder.domain.user.dto.response.LoginResponse;
import devut.buzzerbidder.domain.user.dto.response.UserProfileResponse;
import devut.buzzerbidder.domain.user.entity.User;
import devut.buzzerbidder.domain.user.service.AuthTokenService;
import devut.buzzerbidder.domain.user.service.UserService;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthTokenService authTokenService;
    private final RequestContext requestContext;

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
        setTokensInResponse(response.userInfo().id());
        
        return ApiResponse.ok("로그인에 성공했습니다.", response);
    }


    @Operation(summary = "AccessToken 재발급", description = "Refresh Token을 사용하여 Access Token을 재발급합니다.")
    @PostMapping("/refresh")
    public ApiResponse<Void> refresh() {
        String refreshToken = requestContext.getCookieValue("refreshToken", "");

        // AuthTokenService에서 검증 및 User 조회
        User user = authTokenService.validateAndGetUserByRefreshToken(refreshToken);

        // 새로운 토큰 생성 및 설정
        setTokensInResponse(user.getId());

        return ApiResponse.ok("AccessToken 재발급에 성공했습니다.");
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile() {
        User currentUser = requestContext.getCurrentUser();
        UserProfileResponse response = userService.getMyProfile(currentUser);
        return ApiResponse.ok("회원정보 조회 성공", response);
    }

    @Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 정보를 수정합니다.")
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserUpdateRequest request) {
        User currentUser = requestContext.getCurrentUser();
        UserProfileResponse response = userService.updateMyProfile(currentUser, request);
        return ApiResponse.ok("회원정보 수정 성공", response);
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

package devut.buzzerbidder.domain.member.controller;

import devut.buzzerbidder.domain.member.dto.MemberRequestDto;
import devut.buzzerbidder.domain.member.dto.MemberResponseDto;
import devut.buzzerbidder.domain.member.entity.Member;
import devut.buzzerbidder.domain.member.service.AuthTokenService;
import devut.buzzerbidder.domain.member.service.MemberService;
import devut.buzzerbidder.global.requestcontext.RequestContext;
import devut.buzzerbidder.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final AuthTokenService authTokenService;
    private final RequestContext requestContext;

    @PostMapping("/signup")
    public ApiResponse<MemberResponseDto.LoginResponse> signUp(
            @Valid @RequestBody MemberRequestDto.EmailSignUpRequest request) {
        MemberResponseDto.LoginResponse response = memberService.signUp(request);

        // 토큰 생성 및 헤더/쿠키에 설정
        setTokensInResponse(response.memberInfo().id());
        
        return ApiResponse.ok("회원가입에 성공했습니다.", response);
    }

    @PostMapping("/login")
    public ApiResponse<MemberResponseDto.LoginResponse> login(
            @Valid @RequestBody MemberRequestDto.EmailLoginRequest request) {
        MemberResponseDto.LoginResponse response = memberService.login(request);
        
        // 토큰 생성 및 헤더/쿠키에 설정
        setTokensInResponse(response.memberInfo().id());
        
        return ApiResponse.ok("로그인에 성공했습니다.", response);
    }
    
    private void setTokensInResponse(Long memberId) {
        // Member 조회
        Member member = memberService.findById(memberId);
        
        // 토큰 생성
        String accessToken = authTokenService.genAccessToken(member);
        String refreshToken = authTokenService.genRefreshToken(member);
        
        // 헤더에 토큰 설정
        requestContext.setHeader("Authorization", "Bearer " + accessToken);
        requestContext.setHeader("Refresh-Token", refreshToken);
        
        // 쿠키에 토큰 설정
        requestContext.setCookie("accessToken", accessToken);
        requestContext.setCookie("refreshToken", refreshToken);
    }

}

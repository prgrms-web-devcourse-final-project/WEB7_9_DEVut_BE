package devut.buzzerbidder.domain.member.service;

import devut.buzzerbidder.domain.member.dto.MemberRequestDto;
import devut.buzzerbidder.domain.member.dto.MemberResponseDto;
import devut.buzzerbidder.domain.member.entity.Member;
import devut.buzzerbidder.domain.member.repository.MemberRepository;
import devut.buzzerbidder.global.exeption.BusinessException;
import devut.buzzerbidder.global.exeption.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponseDto.LoginResponse signUp(MemberRequestDto.EmailSignUpRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATE);
        }

        // 닉네임 중복 체크
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATE);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 회원 생성
        Member member = Member.builder()
                .email(request.email())
                .password(encodedPassword)
                .name(request.name())
                .nickname(request.nickname())
                .birthDate(request.birthDate())
                .profileImageUrl(request.profileImageUrl())
                .role(Member.MemberRole.USER)
                .providerType(Member.ProviderType.EMAIL)
                .build();

        memberRepository.save(member);

        return MemberResponseDto.LoginResponse.of(member);
    }

    public MemberResponseDto.LoginResponse login(MemberRequestDto.EmailLoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_LOGIN_FAILED));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_FAILED);
        }

        return MemberResponseDto.LoginResponse.of(member);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}

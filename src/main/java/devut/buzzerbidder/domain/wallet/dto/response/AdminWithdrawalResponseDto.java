package devut.buzzerbidder.domain.wallet.dto.response;

import devut.buzzerbidder.domain.wallet.dto.AdminWithdrawalItemDto;
import devut.buzzerbidder.domain.wallet.entity.Withdrawal;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminWithdrawalResponseDto(
        List<AdminWithdrawalItemDto> withdrawals,
        Integer totalPage,
        Long totalElements,
        Integer currentPage
) {
    public static AdminWithdrawalResponseDto from(List<AdminWithdrawalItemDto> withdrawals, Page<Withdrawal> page)
    {
        return new AdminWithdrawalResponseDto(
                withdrawals,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber()
        );
    }
}

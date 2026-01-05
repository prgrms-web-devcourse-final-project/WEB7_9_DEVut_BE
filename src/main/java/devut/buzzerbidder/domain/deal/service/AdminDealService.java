package devut.buzzerbidder.domain.deal.service;

import devut.buzzerbidder.domain.deal.dto.AdminDelayedDealItemDto;
import devut.buzzerbidder.domain.deal.dto.AdminLiveDealItemDto;
import devut.buzzerbidder.domain.deal.dto.response.AdminDelayedDealResponseDto;
import devut.buzzerbidder.domain.deal.dto.response.AdminLiveDealResponseDto;
import devut.buzzerbidder.domain.deal.entity.DelayedDeal;
import devut.buzzerbidder.domain.deal.entity.LiveDeal;
import devut.buzzerbidder.domain.deal.enums.DealStatus;
import devut.buzzerbidder.domain.deal.repository.DelayedDealRepository;
import devut.buzzerbidder.domain.deal.repository.LiveDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDealService {

    private final DelayedDealRepository delayedDealRepository;
    private final LiveDealRepository liveDealRepository;

    public AdminDelayedDealResponseDto getDelayedDeals(Long buyerId, DealStatus status, Integer page, Integer size) {
        int lastPage = (page == null || page < 0) ? 0 : page;
        int lastSize = (size == null || size <= 0) ? 15 : size;

        Page<DelayedDeal> dealPage;
        Pageable pageable = PageRequest.of(lastPage, lastSize, Sort.by(Sort.Direction.DESC, "createDate"));

        if (buyerId == null && status == null) {
            dealPage = delayedDealRepository.findAll(pageable); // 전체 유저의 전체 상태 조회
        } else if (buyerId == null) {
            dealPage = delayedDealRepository.findByStatus(status, pageable); // 전체 유저의 해당 상태만 조회
        } else if (status == null) {
            dealPage = delayedDealRepository.findByBuyerId(buyerId, pageable); // 해당 유저의 전체 상태 조회
        } else {
            dealPage = delayedDealRepository.findByBuyerIdAndStatus(buyerId, status, pageable); // 해당 유저의 해당 상태만 조회
        }

        List<AdminDelayedDealItemDto> deals = dealPage.getContent().stream()
                .map(AdminDelayedDealItemDto::from)
                .toList();

        return AdminDelayedDealResponseDto.from(deals, dealPage);
    }

    public AdminLiveDealResponseDto getLiveDeals(Long buyerId, DealStatus status, Integer page, Integer size) {
        int lastPage = (page == null || page < 0) ? 0 : page;
        int lastSize = (size == null || size <= 0) ? 15 : size;

        Page<LiveDeal> dealPage;
        Pageable pageable = PageRequest.of(lastPage, lastSize, Sort.by(Sort.Direction.DESC, "createDate"));

        if (buyerId == null && status == null) {
            dealPage = liveDealRepository.findAll(pageable); // 전체 유저의 전체 상태 조회
        } else if (buyerId == null) {
            dealPage = liveDealRepository.findByStatus(status, pageable); // 전체 유저의 해당 상태만 조회
        } else if (status == null) {
            dealPage = liveDealRepository.findByBuyerId(buyerId, pageable); // 해당 유저의 전체 상태 조회
        } else {
            dealPage = liveDealRepository.findByBuyerIdAndStatus(buyerId, status, pageable); // 해당 유저의 해당 상태만 조회
        }

        List<AdminLiveDealItemDto> deals = dealPage.getContent().stream()
                .map(AdminLiveDealItemDto::from)
                .toList();

        return AdminLiveDealResponseDto.from(deals, dealPage);
    }
}

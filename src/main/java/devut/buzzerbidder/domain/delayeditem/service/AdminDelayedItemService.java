package devut.buzzerbidder.domain.delayeditem.service;

import devut.buzzerbidder.domain.delayeditem.dto.AdminDelayedItemDto;
import devut.buzzerbidder.domain.delayeditem.dto.response.AdminDelayedItemResponseDto;
import devut.buzzerbidder.domain.delayeditem.entity.DelayedItem;
import devut.buzzerbidder.domain.delayeditem.repository.DelayedItemRepository;
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
public class AdminDelayedItemService {

    private final DelayedItemRepository delayedItemRepository;

    public AdminDelayedItemResponseDto getDelayedItemsForAdmin(Long sellerUserId, DelayedItem.Category category, Integer page, Integer size){
        int lastPage = (page == null || page < 0) ? 0 : page;
        int lastSize = (size == null || size <= 0) ? 15 : size;

        Page<DelayedItem> dealPage;
        Pageable pageable = PageRequest.of(lastPage, lastSize, Sort.by(Sort.Direction.DESC, "createDate"));

        if (sellerUserId == null && category == null) {
            dealPage = delayedItemRepository.findAll(pageable); // 전체 유저의 전체 상태 조회
        } else if (sellerUserId == null) {
            dealPage = delayedItemRepository.findByCategory(category, pageable); // 전체 유저의 해당 상태만 조회
        } else if (category == null) {
            dealPage = delayedItemRepository.findBySellerUserId(sellerUserId, pageable); // 해당 유저의 전체 상태 조회
        } else {
            dealPage = delayedItemRepository.findBySellerUserIdAndCategory(sellerUserId, category, pageable); // 해당 유저의 해당 상태만 조회
        }

        List<AdminDelayedItemDto> items = dealPage.getContent().stream()
                .map(AdminDelayedItemDto::from)
                .toList();

        return AdminDelayedItemResponseDto.from(items, dealPage);
    }
}

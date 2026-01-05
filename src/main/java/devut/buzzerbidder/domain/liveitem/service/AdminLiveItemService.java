package devut.buzzerbidder.domain.liveitem.service;

import devut.buzzerbidder.domain.liveitem.dto.response.AdminLiveItemDto;
import devut.buzzerbidder.domain.liveitem.dto.response.AdminLiveItemResponseDto;
import devut.buzzerbidder.domain.liveitem.entity.LiveItem;
import devut.buzzerbidder.domain.liveitem.repository.LiveItemRepository;
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
public class AdminLiveItemService {

    private final LiveItemRepository liveItemRepository;

    public AdminLiveItemResponseDto getLiveItemsForAdmin(Long sellerUserId, LiveItem.Category category, Integer page, Integer size){
        int lastPage = (page == null || page < 0) ? 0 : page;
        int lastSize = (size == null || size <= 0) ? 15 : size;

        Page<LiveItem> dealPage;
        Pageable pageable = PageRequest.of(lastPage, lastSize, Sort.by(Sort.Direction.DESC, "createDate"));

        if (sellerUserId == null && category == null) {
            dealPage = liveItemRepository.findAll(pageable); // 전체 유저의 전체 상태 조회
        } else if (sellerUserId == null) {
            dealPage = liveItemRepository.findByCategory(category, pageable); // 전체 유저의 해당 상태만 조회
        } else if (category == null) {
            dealPage = liveItemRepository.findBySellerUserId(sellerUserId, pageable); // 해당 유저의 전체 상태 조회
        } else {
            dealPage = liveItemRepository.findBySellerUserIdAndCategory(sellerUserId, category, pageable); // 해당 유저의 해당 상태만 조회
        }

        List<AdminLiveItemDto> items = dealPage.getContent().stream()
                .map(AdminLiveItemDto::from)
                .toList();

        return AdminLiveItemResponseDto.from(items, dealPage);
    }
}

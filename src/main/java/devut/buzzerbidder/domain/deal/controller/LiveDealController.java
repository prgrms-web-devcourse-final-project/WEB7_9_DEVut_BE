package devut.buzzerbidder.domain.deal.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/me")
@RequiredArgsConstructor
@Tag(name = "LiveDeal", description = "라이브 경매 물품 거래 api")
public class LiveDealController {

}

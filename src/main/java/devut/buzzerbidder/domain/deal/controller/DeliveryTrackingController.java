package devut.buzzerbidder.domain.deal.controller;

import devut.buzzerbidder.domain.deal.dto.DeliveryTrackingResponse;
import devut.buzzerbidder.domain.deal.service.DeliveryTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/tracking")
public class DeliveryTrackingController {

    private final DeliveryTrackingService trackingService;

    public DeliveryTrackingController(DeliveryTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{carrierId}/{trackingNumber}")
    public ResponseEntity<DeliveryTrackingResponse> track(
            @PathVariable String carrierId,
            @PathVariable String trackingNumber
    ) throws IOException {
        DeliveryTrackingResponse response = trackingService.getDeliveryInfo(
                System.getenv("TRACKER_CLIENT_ID"),
                System.getenv("TRACKER_CLIENT_SECRET"),
                carrierId,
                trackingNumber
        );
        return ResponseEntity.ok(response);
    }
}

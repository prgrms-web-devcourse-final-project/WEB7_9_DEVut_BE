package devut.buzzerbidder.domain.payment.infrastructure.tosspayments;

import devut.buzzerbidder.domain.payment.dto.request.PaymentCancelRequestDto;
import devut.buzzerbidder.domain.payment.dto.request.PaymentConfirmRequestDto;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.request.TossCancelRequestDto;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.request.TossConfirmRequestDto;
import devut.buzzerbidder.domain.payment.infrastructure.tosspayments.dto.response.TossConfirmResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TossPaymentsClient {

    @Value("${toss.secret-key}")
    private String secretKey;

    private RestTemplate restTemplate = new RestTemplate();

    public TossConfirmResponseDto confirmPayment(PaymentConfirmRequestDto requestDto) {

        // 토스에서 요구하는 Basic Auth 형식으로 secretKey 인코딩(Base64)
        String encodedSecretKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedSecretKey);

        TossConfirmRequestDto body = new TossConfirmRequestDto(
                requestDto.paymentKey(),
                requestDto.orderId(),
                requestDto.amount()
        );
        HttpEntity<TossConfirmRequestDto> request = new HttpEntity<>(body, headers);

        ResponseEntity<TossConfirmResponseDto> response = restTemplate.postForEntity(
                "https://api.tosspayments.com/v1/payments/confirm",
                request,
                TossConfirmResponseDto.class
        );

        return response.getBody();
    }

    public void cancelPayment(String paymentKey, PaymentCancelRequestDto requestDto) {
        String encodedSecretKey = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedSecretKey);

        TossCancelRequestDto body = new TossCancelRequestDto(
                requestDto.cancelReason()
        );

        String url = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";
        HttpEntity<TossCancelRequestDto> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, void.class);
    }
}

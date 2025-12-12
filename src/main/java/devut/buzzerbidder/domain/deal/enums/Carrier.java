package devut.buzzerbidder.domain.deal.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum Carrier {

    CJ_LOGISTICS("kr.cjlogistics", "CJ대한통운"),
    COUPANG_LS("kr.coupangls", "쿠팡 로지스틱스 서비스"),
    CU_POST("kr.cupost", "CU 편의점택배"),
    CHUNIL("kr.chunilps", "천일택배"),
    GS_POSTBOX("kr.cvsnet", "GS Postbox"),
    DAESIN("kr.daesin", "대신택배"),
    PANTOS("kr.epantos", "LX 판토스"),
    KOREA_POST("kr.epost", "우체국택배"),
    HOMEPICK("kr.homepick", "홈픽"),
    HANJIN("kr.hanjin", "한진택배"),
    ILYANG("kr.ilyanglogis", "일양로지스");

    private final String code;
    private final String displayName;
    private static final Map<String, Carrier> CODE_MAP =
            Stream.of(values())
                    .collect(Collectors.toMap(Carrier::getCode, c -> c));

    public static Carrier fromCode(String code) {
        Carrier carrier = CODE_MAP.get(code);
        if (carrier == null) {
            throw new IllegalArgumentException("Unknown carrier code: " + code);
        }
        return carrier;
    }

}

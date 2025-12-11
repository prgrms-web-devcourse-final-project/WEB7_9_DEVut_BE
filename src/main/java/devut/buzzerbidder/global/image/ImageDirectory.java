package devut.buzzerbidder.global.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * S3 이미지 업로드 디렉토리 enum
 */
@Getter
@RequiredArgsConstructor
public enum ImageDirectory {

    AUCTIONS("auctions", "경매 상품 이미지"),
    PROFILES("profiles", "프로필 이미지"),
    CHATS("chats", "채팅 이미지");

    private final String path;
    private final String description;

    /**
     * path 문자열로부터 ImageDirectory 찾기
     *
     * @param path 디렉토리 경로 (예: "auctions", "profiles")
     * @return 해당하는 ImageDirectory enum
     * @throws IllegalArgumentException 유효하지 않은 경로인 경우
     */
    public static ImageDirectory fromPath(String path) {
        for (ImageDirectory directory : values()) {
            if (directory.path.equals(path)) {
                return directory;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 디렉토리: " + path);
    }

    /**
     * 전체 S3 키 생성
     *
     * @param fileName 파일명
     * @return S3 키 (예: "auctions/filename.jpg")
     */
    public String createS3Key(String fileName) {
        return path + "/" + fileName;
    }
}

package devut.buzzerbidder.global.image;

public class ImageFileUtils {

    private ImageFileUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String[] SUPPORTED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * 파일 확장자 추출
     *
     * @param filename 파일명
     * @return 소문자로 변환된 확장자 (확장자가 없으면 빈 문자열)
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int lastIndexOf = filename.lastIndexOf('.');
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1).toLowerCase();
    }

    /**
     * 지원하는 이미지 확장자인지 확인
     *
     * @param extension 확장자
     * @return 지원 여부
     */
    public static boolean isSupportedExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }

        String ext = extension.toLowerCase();
        for (String supported : SUPPORTED_EXTENSIONS) {
            if (supported.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 파일 확장자에 맞는 Content-Type 반환
     *
     * @param extension 파일 확장자
     * @return Content-Type
     */
    public static String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}

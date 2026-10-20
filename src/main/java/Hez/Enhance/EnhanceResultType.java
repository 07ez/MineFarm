package Hez.Enhance;

public enum EnhanceResultType {
    SUCCESS("강화에 성공했습니다!"),
    FAILURE("강화에 실패했습니다."),
    DESTROY("강화에 실패하여 아이템이 파괴되었습니다!"),
    MAX_LEVEL("이미 최고 강화 단계입니다."),
    INVALID_ITEM("강화할 수 없는 아이템입니다.");

    private final String defaultMessage;

    EnhanceResultType(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}

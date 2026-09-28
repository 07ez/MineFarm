package Hez.Job;

public enum JobType{
    // 직업 종류
    // 메인직업
    none("직업없음", true),
    farmer("농부", true),
    fisherman("어부", true),
    miner("광부", true),
    carpenter("목수", true),

    // 히든직업
    gambler("도박사", false),
    chef("요리사", false),
    robber("강도", false),
    Sheriff("보안관", false),
    thief("도둑", false);

    private final String koreanName;
    private final boolean primary;

    JobType(String koreanName, boolean primary) {
        this.koreanName = koreanName;
        this.primary = primary;
    }

    // 한글 이름
    public String getKoreanName() {
        return koreanName;
    }
    public boolean isPrimary() {
        return primary;
    }

    public static JobType fromString(String text) {
        for (JobType type : values()) {
            if (type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        return none;
    }
}

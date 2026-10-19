package Hez.Quest;

public enum QuestStatus {
    LOCKED("§7● 상태: 잠김"),
    IN_PROGRESS("§e● 상태: 진행중"),
    COMPLETED("§a● 상태: 클리어");

    private final String formattedText;

    QuestStatus(String formattedText) {
        this.formattedText = formattedText;
    }

    public String getFormattedText() {
        return formattedText;
    }
}

package Hez.Casino;

public enum CasinoType {
    ROULETTE("wheel", "\uE501", "§6§l카지노 룰렛 (Roulette)", 50),
    HOLDEM("holdem", "\uE540", "§c§l텍사스 홀덤 (Texas Hold'em)", 1),
    SLOT_MACHINE("slot", "\uE500", "§e§l슬롯머신 (Slot Machine)", 48);

    private final String tag;
    private final String unicode;
    private final String title;
    private final int moneySlot;

    CasinoType(String tag, String unicode, String title, int moneySlot) {
        this.tag = tag;
        this.unicode = unicode;
        this.title = title;
        this.moneySlot = moneySlot;
    }

    public String getTag() {
        return tag;
    }

    public String getUnicode() {
        return unicode;
    }

    public String getTitle() {
        return title;
    }

    public int getMoneySlot() {
        return moneySlot;
    }

    public static CasinoType fromTag(String tag) {
        if (tag == null) return null;
        for (CasinoType type : values()) {
            if (type.tag.equalsIgnoreCase(tag)) {
                return type;
            }
        }
        return null;
    }
}

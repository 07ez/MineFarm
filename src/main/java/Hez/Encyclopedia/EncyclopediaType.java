package Hez.Encyclopedia;

public enum EncyclopediaType {
    DEFAULT("기본도감"),
    FISH("물고기도감"),
    COOK("요리도감");

    private final String displayName;

    EncyclopediaType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
